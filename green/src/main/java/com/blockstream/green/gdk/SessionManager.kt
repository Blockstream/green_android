package com.blockstream.green.gdk

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.gdk.AssetManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.GreenWallet.Companion.JsonDeserializer
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletId
import com.blockstream.green.devices.Device
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.logException
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KLogging
import java.util.*
import javax.inject.Provider
import kotlin.collections.set
import kotlin.concurrent.schedule
import kotlin.properties.Delegates

class SessionManager constructor(
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private var countlyProvider: Provider<Countly>,
    private val greenWallet: GreenWallet,
    qaTester: QATester,
) : DefaultLifecycleObserver {

    private val countly by lazy { countlyProvider.get() }

    private val torNetworkSession : GreenSession by lazy {
        GreenSession(
            sessionManager = this,
            settingsManager = settingsManager,
            assetManager = assetManager,
            greenWallet = greenWallet,
            countly = countly
        )
    }

    private var torEnabled : Boolean by Delegates.observable(settingsManager.getApplicationSettings().tor) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if (newValue) {
                startTorNetworkSessionIfNeeded()
            } else {
                _torProxy.value = null
                torNetworkSession.disconnectAsync()
            }
        }
    }

    private val _torProxy : MutableStateFlow<String?> = MutableStateFlow(null)
    val torProxy
        get() = _torProxy.asStateFlow()

    private val greenSessions = mutableSetOf<GreenSession>()
    private val walletSessions = mutableMapOf<WalletId, GreenSession>()
    private var onBoardingSession: GreenSession? = null

    private var isOnForeground: Boolean = false

    private var timeoutTimers = mutableListOf<Timer>()

    var pendingBip21Uri = MutableLiveData<ConsumableEvent<String>>()

    var connectionChangeEvent = MutableLiveData<Boolean>()

    var hardwareWallets = MutableLiveData<List<Wallet>>(listOf())
    var ephemeralWallets = MutableLiveData<List<Wallet>>(listOf())

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        settingsManager.getApplicationSettingsLiveData().observeForever {
            torEnabled = it.tor
        }

        greenWallet.setNotificationHandler { gaSession, jsonObject ->
            greenSessions.find { it.gaSession == gaSession }?.apply {
                try{
                    onNewNotification(JsonDeserializer.decodeFromJsonElement(jsonObject as JsonElement))
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        // Inject notification events from QATester to all sessions
        qaTester.getSessionNotificationInjectorObservable().subscribeBy { notification ->
            for(session in greenSessions){
                session.onNewNotification(notification)
            }
        }

        connectionChangeEvent.observeForever {
            getConnectedHardwareWalletSessions().mapNotNull { it.ephemeralWallet }.let {
                hardwareWallets.postValue(it)
            }

            getConnectedEphemeralWalletSessions().mapNotNull { it.ephemeralWallet }.let {
                ephemeralWallets.postValue(it)
            }
        }
    }

    fun getDeviceSessionForNetwork(device: Device, networkId: String): GreenSession? {
        return greenSessions.find { it.device == device && it.network.id == networkId }
    }

    fun getWalletSessionOrNull(wallet: Wallet): GreenSession? {
        return getWalletSessionOrNull(wallet.id)
    }

    fun getWalletSession(wallet: Wallet): GreenSession {
        return getWalletSession(wallet.id)
    }

    fun getWalletSessionOrNull(walletId: WalletId): GreenSession? {
        if(walletId < 0){
            greenSessions.find { it.ephemeralWallet?.id == walletId }?.let {
                return it
            }
        }

        return walletSessions[walletId]
    }

    private fun getWalletSession(walletId: WalletId): GreenSession {
        return getWalletSessionOrNull(walletId) ?: createSession().also {
            walletSessions[walletId] = it
        }
    }

    fun getWalletIdFromSession(session: GreenSession): WalletId? {
        return walletSessions.filterValues { it == session }.keys.firstOrNull()
    }

    fun getSessions() : Set<GreenSession> = greenSessions

    fun destroyWalletSession(wallet: Wallet){
        walletSessions[wallet.id]?.let{
            it.destroy()
            greenSessions.remove(it)
        }

        walletSessions.remove(wallet.id)
    }

    fun destroyHardwareSession(greenSession: GreenSession){
        // Remove from greenSessions
        greenSessions.remove(greenSession)

        // Remove from walletSessions
        greenSession.ephemeralWallet?.let { walletSessions.remove(it.id) }

        greenSession.destroy()

        // Disconnect device if there is no other connected session with that device
        if(getConnectedHardwareWalletSessions().none { it.device?.id == greenSession.device?.id }){
            greenSession.device?.disconnect()
        }
    }

    private fun getConnectedHardwareWalletSessions(): List<GreenSession>{
        return walletSessions.values.filter { it.ephemeralWallet?.isHardware == true && it.isConnected }.toList()
    }

    private fun getConnectedEphemeralWalletSessions(): List<GreenSession>{
        return walletSessions.values.filter { it.ephemeralWallet?.isHardware == false && it.isConnected }.toList()
    }

    fun getConnectedDevices(): List<Device>{
        return walletSessions.values
            .filter { it.device?.isOffline == false }
            .mapNotNull { it.device }
            .toList()
    }

    // OnBoardingSession waits patiently to be upgraded to a proper wallet session
    fun getOnBoardingSession(wallet: Wallet? = null): GreenSession {
        wallet?.let {
            getWalletSessionOrNull(wallet)?.let {
                return it
            }
        }

        // Create a new session if doesn't exists
        return (onBoardingSession ?: createSession().also {
            onBoardingSession = it
        })
    }

    fun getConnectedSessions(): List<GreenSession> {
        return greenSessions.filter { it.isConnected }
    }

    fun upgradeOnBoardingSessionToWallet(wallet: Wallet) {
        onBoardingSession?.let {
            walletSessions[wallet.id] = it
            // fire connection change event so that all listeners can track the new session status
            fireConnectionChangeEvent()
            onBoardingSession = null
        }
    }

    // Always use this method to create a Session as it keeps track of gaSession & GreenSession
    private fun createSession(): GreenSession {
        val session = GreenSession(
            sessionManager = this,
            settingsManager = settingsManager,
            assetManager = assetManager,
            greenWallet = greenWallet,
            countly = countly
        )

        greenSessions.add(session)

        return session
    }

    private fun startTorNetworkSessionIfNeeded() {
        val applicationSettings = settingsManager.getApplicationSettings()
        // If user provides a "sock5://", handle it as Orbot proxy, no need to start GDK TOR session
        if (applicationSettings.tor && applicationSettings.proxyUrl?.startsWith("socks5://") != true) {
            if (!torNetworkSession.isConnected) {
                // Re-initiate connection
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    torNetworkSession.connect(greenWallet.networks.bitcoinElectrum)
                    _torProxy.emit(torNetworkSession.getProxySettings().proxy)
                }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        isOnForeground = true
        timeoutTimers.forEach { it.cancel() }
        timeoutTimers.clear()

        startTorNetworkSessionIfNeeded()
    }

    override fun onPause(owner: LifecycleOwner) {
        isOnForeground = false
        for(session in greenSessions.filter { it.isConnected }){
            val sessionTimeout = (session.getSettings()?.altimeout ?: 1) * 60 * 1000L

            timeoutTimers += Timer().also {
                it.schedule(sessionTimeout) {
                    logger.debug { "Session timeout, disconnecting..." }
                    session.disconnectAsync()
                }
            }
        }
    }

    fun fireConnectionChangeEvent(){
        connectionChangeEvent.postValue(true)
    }

    companion object: KLogging()
}
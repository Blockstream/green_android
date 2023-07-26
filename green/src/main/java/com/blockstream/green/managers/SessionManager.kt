package com.blockstream.green.managers

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.CountlyInteface
import com.blockstream.common.gdk.GASession
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.TorEvent
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.common.managers.AssetManager
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.ApplicationScope
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletId
import com.blockstream.green.devices.Device
import com.blockstream.green.extensions.logException
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.blockstream.lightning.LightningManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KLogging
import java.util.Timer
import kotlin.collections.set
import kotlin.concurrent.schedule
import kotlin.properties.Delegates

class SessionManager constructor(
    private val applicationScope: ApplicationScope,
    private val lifecycleManager: LifecycleManager,
    val lightningManager: LightningManager,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private var countly: CountlyInteface,
    private val gdk: Gdk,
    private val wally: Wally,
    qaTester: QATester,
) {
    val httpRequestProvider : GdkSession by lazy {
        createSession()
    }

    private val torNetworkSession : GdkSession by lazy {
        createSession().also {
            it.torStatusFlow.onEach { torEvent ->
                _torProxyProgress.value = torEvent
            }.launchIn(applicationScope)
        }
    }

    private var torEnabled : Boolean by Delegates.observable(settingsManager.appSettings.tor) { _, oldValue, newValue ->
        if(oldValue != newValue){
            if (newValue) {
                startTorNetworkSessionIfNeeded()
            } else {
                _torProxy.value = null
                _torProxyProgress.value = TorEvent(progress = 100)
                torNetworkSession.disconnectAsync()
            }
        }
    }

    private val _torProxyProgress : MutableStateFlow<TorEvent> = MutableStateFlow(TorEvent(progress = 100))
    val torProxyProgress
        get() = _torProxyProgress.asStateFlow()

    private val _torProxy : MutableStateFlow<String?> = MutableStateFlow(null)
    val torProxy
        get() = _torProxy.asStateFlow()

    private val gdkSessions = mutableSetOf<GdkSession>()
    private val walletSessions = mutableMapOf<WalletId, GdkSession>()
    private var onBoardingSession: GdkSession? = null

    private var timeoutTimers = mutableListOf<Timer>()

    var pendingUri = MutableLiveData<ConsumableEvent<String>?>()

    var connectionChangeEvent = MutableLiveData<Boolean>()

    var hardwareWallets = MutableStateFlow<List<Wallet>>(listOf())
    var ephemeralWallets = MutableStateFlow<List<Wallet>>(listOf())

    init {
        lifecycleManager.lifecycleState.onEach { lifecycle ->
            if (lifecycle.isForeground()) {
                timeoutTimers.forEach { it.cancel() }
                timeoutTimers.clear()

                startTorNetworkSessionIfNeeded()
            } else {
                for (session in gdkSessions.filter { it.isConnected }) {
                    val sessionTimeout = (session.getSettings(null)?.altimeout ?: 1) * 60 * 1000L

                    timeoutTimers += Timer().also {
                        it.schedule(sessionTimeout) {
                            logger.debug { "Session timeout, disconnecting..." }
                            session.disconnectAsync()
                        }
                    }
                }

            }
        }.launchIn(CoroutineScope(context = Dispatchers.Default))

        settingsManager.appSettingsStateFlow.onEach {
            torEnabled = it.tor
        }.launchIn(CoroutineScope(context = Dispatchers.Default))

        gdk.setNotificationHandler { gaSession: GASession, jsonObject: Any ->
            try {
                gdkSessions.forEach {
                    it.onNewNotification(
                        gaSession,
                        JsonDeserializer.decodeFromJsonElement(jsonObject as JsonElement)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Inject notification events from QATester to all sessions
        qaTester.getSessionNotificationInjectorFlow().onEach { notification ->
            for(session in gdkSessions){
                session.gdkSessions.forEach {
                    session.onNewNotification(it.value, notification)
                }
            }
        }.launchIn(applicationScope)

        connectionChangeEvent.observeForever {
            getConnectedEphemeralWalletSessions().filter { it.ephemeralWallet?.isHardware == true }.mapNotNull { it.ephemeralWallet }.let {
                hardwareWallets.value = it
            }

            getConnectedEphemeralWalletSessions().filter { it.ephemeralWallet?.isHardware == false }.mapNotNull { it.ephemeralWallet }.let {
                ephemeralWallets.value = it
            }
        }

        torProxy.filterNotNull().onEach {
            countly.updateTorProxy(it)
        }.launchIn(CoroutineScope(context = Dispatchers.Default))
    }

    fun getDeviceSessionForNetworkAllPolicies(device: Device, network: Network, isEphemeral: Boolean): GdkSession? {
        return gdkSessions.find { it.device == device && it.defaultNetworkOrNull?.isBitcoin == network.isBitcoin && it.defaultNetworkOrNull?.isTestnet == network.isTestnet && (it.ephemeralWallet != null) == isEphemeral}
    }

    fun getDeviceSessionForEnvironment(device: Device, isTestnet: Boolean, isEphemeral: Boolean): GdkSession? {
        return gdkSessions.find { it.device == device && it.isTestnet == isTestnet && (it.ephemeralWallet != null) == isEphemeral }
    }

    fun getWalletSessionOrNull(wallet: Wallet): GdkSession? {
        return getWalletSessionOrNull(wallet.id)
    }

    fun getWalletSession(wallet: Wallet): GdkSession {
        return getWalletSession(wallet.id)
    }

    fun getWalletSessionOrNull(walletId: WalletId): GdkSession? {
        if(walletId < 0){
            gdkSessions.find { it.ephemeralWallet?.id == walletId }?.let {
                return it
            }
        }

        return walletSessions[walletId]
    }

    fun getEphemeralWalletSession(walletHashId: String, isHardware: Boolean = false) =
        gdkSessions.find { it.ephemeralWallet?.walletHashId == walletHashId && it.ephemeralWallet?.isHardware == isHardware }


    private fun getWalletSession(walletId: WalletId): GdkSession {
        return getWalletSessionOrNull(walletId) ?: createSession().also {
            walletSessions[walletId] = it
        }
    }

    fun getWalletIdFromSession(session: GdkSession): WalletId? {
        return walletSessions.filterValues { it == session }.keys.firstOrNull()
    }

    fun getSessions() : Set<GdkSession> = gdkSessions

    fun destroyWalletSession(wallet: Wallet){
        walletSessions[wallet.id]?.let{
            it.destroy()
            gdkSessions.remove(it)
        }

        walletSessions.remove(wallet.id)
    }

    fun destroyEphemeralSession(gdkSession: GdkSession){
        // Remove from greenSessions
        gdkSessions.remove(gdkSession)

        // Remove from walletSessions
        gdkSession.ephemeralWallet?.let { walletSessions.remove(it.id) }

        gdkSession.destroy()
    }

    private fun getConnectedEphemeralWalletSessions(): List<GdkSession>{
        return walletSessions.values.filter { it.ephemeralWallet != null && it.isConnected }.toList()
    }

    fun getConnectedHardwareWalletSessions(): List<GdkSession>{
        return walletSessions.values.filter { it.isHardwareWallet && it.isConnected }.toList()
    }

    fun getNextEphemeralId() : Long = (getConnectedEphemeralWalletSessions().filter { it.ephemeralWallet?.isHardware == false }.mapNotNull { it.ephemeralWallet?.ephemeralId }.maxOrNull() ?: 0) + 1

    fun getConnectedDevices(): List<DeviceInterface>{
        return walletSessions.values
            .filter { it.device?.isOffline == false }
            .mapNotNull { it.device }
            .toList()
    }

    // OnBoardingSession waits patiently to be upgraded to a proper wallet session
    fun getOnBoardingSession(wallet: Wallet? = null): GdkSession {
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

    fun getConnectedSessions(): List<GdkSession> {
        return gdkSessions.filter { it.isConnected }
    }

    // Provide an upgradeSession if not sure if it's a OnBoardingSession
    fun upgradeOnBoardingSessionToWallet(wallet: Wallet) {
        onBoardingSession?.let {
            walletSessions[wallet.id] = it
            // fire connection change event so that all listeners can track the new session status
            fireConnectionChangeEvent()
            onBoardingSession = null
        }
    }

    fun upgradeEphemeralHWSessionToWallet(session: GdkSession, ephemeralWalletId: Long, wallet: Wallet) {
        // Remove old ephemeral wallet session
        walletSessions.remove(ephemeralWalletId)

        walletSessions[wallet.id] = session
        // fire connection change event so that all listeners can track the new session status
        fireConnectionChangeEvent()
    }

    // Always use this method to create a Session as it keeps track of gaSession & GreenSession
    private fun createSession(): GdkSession {
        val session = GdkSession(
            sessionManager = this,
            settingsManager = settingsManager,
            assetManager = assetManager,
            gdk = gdk,
            wally = wally,
            countly = countly
        )

        gdkSessions.add(session)

        return session
    }

    private fun startTorNetworkSessionIfNeeded() {
        val applicationSettings = settingsManager.getApplicationSettings()
        // If user provides a "sock5://", handle it as Orbot proxy, no need to start GDK TOR session
        if (applicationSettings.tor && applicationSettings.proxyUrl?.startsWith("socks5://") != true) {
            if (!torNetworkSession.isConnected) {
                // Re-initiate connection
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    gdk.networks().bitcoinElectrum.also { network ->
                        torNetworkSession.connect(network = network, initNetworks = listOf(network))
                    }
                    _torProxy.value = torNetworkSession.getProxySettings().proxy
                }
            }
        }
    }

    fun fireConnectionChangeEvent(){
        connectionChangeEvent.postValue(true)
    }

    fun disconnectAll(){
        logger.info { "Disconnecting all sessions" }
        getConnectedSessions().onEach {
            it.disconnectAsync()
        }
    }

    companion object: KLogging()
}
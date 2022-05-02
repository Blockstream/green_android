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
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KLogging
import java.util.*
import kotlin.collections.set
import kotlin.concurrent.schedule

class SessionManager constructor(
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val greenWallet: GreenWallet,
    private val countly: Countly,
    qaTester: QATester,
) : DefaultLifecycleObserver {
    private val greenSessions = mutableSetOf<GreenSession>()
    private val walletSessions = mutableMapOf<WalletId, GreenSession>()
    private var onBoardingSession: GreenSession? = null

    private var isOnForeground: Boolean = false

    private var timeoutTimers = mutableListOf<Timer>()

    var pendingBip21Uri = MutableLiveData<ConsumableEvent<String>>()

    var connectionChangeEvent = MutableLiveData<Boolean>()

    var hardwareWallets = MutableLiveData<List<Wallet>>(listOf())

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

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
            getHardwareWalletSessions().filter { it.isConnected }.mapNotNull { it.hardwareWallet }.let {
                hardwareWallets.postValue(it)
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
        return walletSessions[walletId]
    }

    private fun getWalletSession(walletId: WalletId): GreenSession {
        if(walletId < 0){
            greenSessions.find { it.hardwareWallet?.id == walletId }?.let {
                return it
            }
        }

        return getWalletSessionOrNull(walletId) ?: createSession().also {
            walletSessions[walletId] = it
        }
    }

    fun getWalletIdFromSession(session: GreenSession): WalletId {
        for (key in walletSessions.keys){
            if(walletSessions[key] == session){
                return key
            }
        }

        return -1
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
        greenSessions.remove(greenSession)
        greenSession.hardwareWallet?.let { walletSessions.remove(it.id) }
        greenSession.destroy()
    }

    fun getHardwareWalletSessions(): List<GreenSession>{
        return walletSessions.values.filter { it.hardwareWallet?.isHardware == true && it.isConnected }.toList()
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
            applicationScope = applicationScope,
            sessionManager = this,
            settingsManager = settingsManager,
            assetsManager = assetManager,
            greenWallet = greenWallet,
            countly = countly
        )

        greenSessions.add(session)

        return session
    }

    override fun onResume(owner: LifecycleOwner) {
        isOnForeground = true
        timeoutTimers.forEach { it.cancel() }
        timeoutTimers.clear()
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
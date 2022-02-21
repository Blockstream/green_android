package com.blockstream.green.gdk


import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.gdk.AssetManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.GreenWallet.Companion.JsonDeserializer
import com.blockstream.green.ApplicationScope
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletId
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
    qaTester: QATester,
) : DefaultLifecycleObserver {
    private val greenSessions = mutableSetOf<GreenSession>()
    private val walletSessions = mutableMapOf<WalletId, GreenSession>()
    private var onBoardingSession: GreenSession? = null
    private var hardwareSession: GreenSession? = null

    private var isOnForeground: Boolean = false

    private var timeoutTimers = mutableListOf<Timer>()

    var pendingBip21Uri = MutableLiveData<ConsumableEvent<String>>()

    var connectionChangeEvent = MutableLiveData<Boolean>()

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
    }

    fun getWalletSession(wallet: Wallet): GreenSession {
        // If id == -1 is a hardware wallet connection, we emulate it as currently we don't save the Wallet
        if(wallet.isHardwareEmulated){
            return getHardwareSession()
        }

        if (walletSessions[wallet.id] == null) {
            val session = createSession()
            walletSessions[wallet.id] = session
        }

        return walletSessions[wallet.id]!!
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

    fun getHardwareSession(): GreenSession {
        if (hardwareSession == null) {
            hardwareSession = createSession()
        }

        return hardwareSession!!
    }

    fun getOnBoardingSession(wallet: Wallet? = null): GreenSession {
        wallet?.let {
            return getWalletSession(it)
        }

        // OnBoardingSession waits patiently to be upgraded to a proper wallet session

        if (onBoardingSession == null) {
            onBoardingSession = createSession()
        }

        return onBoardingSession!!
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
package com.blockstream.green.gdk

import android.content.Context
import androidx.lifecycle.*
import com.blockstream.gdk.AssetManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.GreenWallet.Companion.JsonDeserializer
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletId
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.blockstream.libgreenaddress.GASession
import com.greenaddress.greenapi.Session
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import mu.KLogging
import java.util.*
import kotlin.concurrent.schedule

class SessionManager constructor(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val assetManager: AssetManager,
    private val greenWallet: GreenWallet,
    qaTester: QATester,
) : LifecycleObserver {
    private val sessions = mutableMapOf<GASession, GreenSession>()
    private val walletSessions = mutableMapOf<WalletId, GreenSession>()
    private var onBoardingSession: GreenSession? = null
    private var jadeSession: GreenSession? = null
    private var hardwareSessionV3: GreenSession? = null

    private var timeoutTimers = mutableListOf<Timer>()

    var pendingBip21Uri = MutableLiveData<ConsumableEvent<String>>()

    var connectionChangeEvent = MutableLiveData<Boolean>()

    private val AllowMultipleConnectedSessions = true

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        greenWallet.setNotificationHandler { gaSession, jsonObject ->
            sessions[gaSession]?.apply {

                try{
                    // Pass notification to to GDKSession
                    Session.getSession().also {
                        if(it.nativeSession == gaSession){
                            it.notificationModel.onNewNotification(gaSession, jsonObject)
                        }
                    }

                    onNewNotification(JsonDeserializer.decodeFromJsonElement(jsonObject as JsonElement))
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        // Inject notification events from QATester to all sessions
        qaTester.getSessionNotificationInjectorObservable().subscribeBy { notification ->
            for(session in sessions.values){
                session.onNewNotification(notification)
            }

            // Pass notification to to GDKSession
            Session.getSession().also {
                it.notificationModel.onNewNotification(it.nativeSession, JsonDeserializer.encodeToJsonElement(notification))
            }
        }
    }

    fun getWalletSession(wallet: Wallet): GreenSession {
        // If id == -1 is a hardware wallet connection, we emulate it as currently we don't save the Wallet
        if(wallet.isHardwareEmulated){
            return getHardwareSessionV3()
        }

        if (walletSessions[wallet.id] == null) {
            val session = createSession()
            walletSessions[wallet.id] = session
        }

        return walletSessions[wallet.id]!!
    }

    // Used to find a GreenSession from v3 gaSession
    fun getWalletSession(gaSession: GASession?): GreenSession? {
        return sessions[gaSession]
    }

    // Used to find the Wallet Id from v3 gaSession
    fun getWalletIdFromSession(gaSession: GASession?): WalletId {
        getWalletSession(gaSession)?.let { greenSession ->
            for (key in walletSessions.keys){
                if(walletSessions[key] == greenSession){
                    return key
                }
            }
        }

        return -1
    }

    fun destroyWalletSession(wallet: Wallet){
        walletSessions[wallet.id]?.let{
            it.destroy()
            sessions.remove(it.gaSession)
        }

        walletSessions.remove(wallet.id)
    }

    fun getHardwareSessionV3(): GreenSession {
        if (hardwareSessionV3 == null) {
            hardwareSessionV3 = createSession()
        }

        return hardwareSessionV3!!
    }

    fun getOnBoardingSession(wallet: Wallet? = null): GreenSession {
        wallet?.let {
            return getWalletSession(it)
        }

        // OnBoardingSession waits petiently to be upgraded to a proper wallet session

        if (onBoardingSession == null) {
            onBoardingSession = createSession()
        }

        return onBoardingSession!!
    }

    // Is this really needed
    fun getJadeSession(): GreenSession {
        if (jadeSession == null) {
            jadeSession = createSession()
        }

        return jadeSession!!
    }

    fun getConnectedSessions(): Map<GASession, GreenSession> {
        return sessions.filter { it.value.isConnected }
    }

    fun upgradeOnBoardingSessionToWallet(wallet: Wallet) {
        onBoardingSession?.let {
            walletSessions[wallet.id] = it
            onBoardingSession = null
        }
    }

    // Always use this method to create a Session as it keeps track of gaSession & GreenSession
    private fun createSession(): GreenSession {
        val session = GreenSession(this, settingsManager, assetManager, greenWallet)

        sessions[session.gaSession] = session

        return session
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onEnterForeground() {
        timeoutTimers.forEach { it.cancel() }
        timeoutTimers.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onEnterBackground() {
        for(session in sessions.filter { it.value.isConnected }.values){

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

    fun disconnectSessions(exception: GreenSession){
        if(!AllowMultipleConnectedSessions) {
            for (session in sessions.values) {
                if (session != exception) {
                    if (session.isConnected) {
                        session.disconnectAsync()
                    }
                }
            }
        }
    }

    companion object: KLogging()
}
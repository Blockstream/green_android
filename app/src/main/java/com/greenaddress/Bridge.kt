package com.greenaddress

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.blockstream.crypto.BuildConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.greenapi.Session
import com.greenaddress.greenbits.spv.SPV
import com.greenaddress.greenbits.ui.preferences.PrefKeys
import com.greenaddress.jade.JadeAPI
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.lang.ref.WeakReference
import java.util.*

// Bridge object is used to make the transition to Green Prototype
object Bridge {

    private lateinit var context: WeakReference<Context>

    const val useGreenModule = false
    
    var isDebug = false
        private set

    var versionName = "0.0.0"
        private set

    val spv = SPV()

    private var navigateFn: ((activity: FragmentActivity, type: NavigateType, gaSession: Any?) -> Unit)? = null
    private var setSubaccountFn: ((gaSession: Any?, subaccount: Int) -> Unit)? = null
    private var getSubaccountFn: ((gaSession: Any?) -> Int)? = null
    private var walletsProviderFn: ((gaSession: Any?) -> List<HashMap<String, String>>)? = null
    private var recoveryConfirmedProviderFn: ((gaSession: Any?) -> Boolean)? = null
    private var connectFn: ((context: Context, gaSession: Any?, networkId: String) -> Unit)? = null

    private var initialized = false

    enum class NavigateType {
        LOGOUT, CHANGE_PIN, CONNECTION_SETTINGS, BACKUP_RECOVERY, TWO_FACTOR_RESET
    }

    fun initializeBridge(
        ctx: Context,
        debug: Boolean,
        version: String
    ){
        if(!initialized) {
            initialized = true

            context = WeakReference(ctx.applicationContext)

            isDebug = debug
            versionName = version

            JadeAPI.isDebug = BuildConfig.DEBUG
        }
    }

    fun setNavigateHandler(fn: ((activity: FragmentActivity, type: NavigateType, gaSession: Any?) -> Unit)){
        navigateFn = fn
    }

    fun setWalletProvider(fn : ((gaSession: Any?) -> List<HashMap<String,String>>)){
        walletsProviderFn = fn
    }

    fun setRecoveryConfirmedProvider(fn : ((gaSession: Any?) -> Boolean)){
        recoveryConfirmedProviderFn = fn
    }

    fun setActiveAccountHandler(fn : ((gaSession: Any?, subaccount: Int) -> Unit)){
        setSubaccountFn = fn
    }

    fun setGetActiveAccountHandler(fn: (gaSession: Any?) -> Int){
        getSubaccountFn = fn
    }

    fun setConnectHandler(fn: (context:Context, gaSession: Any?, networkId: String) -> Unit){
        connectFn = fn
    }

    fun connect(context:Context, gaSession: Any?, network: String){
        connectFn?.invoke(context, gaSession, network)
    }

    fun getIsRecoveryConfirmed() = recoveryConfirmedProviderFn?.invoke(Session.getSession().nativeSession) ?: true

    fun getWallets() = walletsProviderFn?.invoke(Session.getSession().nativeSession)

    fun navigateToLogin(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.LOGOUT, Session.getSession().nativeSession)
    }

    fun navigateToChangePin(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.CHANGE_PIN, Session.getSession().nativeSession)
    }

    fun navigateToBackupRecovery(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.BACKUP_RECOVERY, Session.getSession().nativeSession)
    }

    fun twoFactorResetDialog(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_RESET, Session.getSession().nativeSession)
    }

    fun bridgeSession(session: Any, networkId: String, watchOnlyUsername: String?) {
        Session.getSession().bridgeSession(session, networkId, watchOnlyUsername)
        setCurrentNetwork(context.get()!!, networkId)
    }

    fun setActiveAccount(account : Int){
        setSubaccountFn?.let {
            it.invoke(Session.getSession().nativeSession, account)
        }
    }

    fun getActiveAccount(): Int {
        return getSubaccountFn?.let {

            val session = Session.getSession()

            if(session != null && session.nativeSession != null){
                session.nativeSession.let { nativeSession ->
                    return@getActiveAccount it.invoke(nativeSession)
                }
            }

            0
        } ?: 0
    }

    fun setCurrentNetwork(context: Context, networkId: String) = BridgeJava.setCurrentNetwork(
        context,
        networkId
    )

    fun getCurrentNetworkData(context: Context) = BridgeJava.getCurrentNetworkData(context)

    fun getCurrentNetwork(context: Context) = BridgeJava.getCurrentNetwork(context)

    fun startSpvServiceIfNeeded(context: Context){
        // check and start spv if enabled
        // setup data observers
        val networkData = getCurrentNetworkData(context)
        val preferences: SharedPreferences = context.getSharedPreferences(networkData.network, Context.MODE_PRIVATE)

        // check and start spv if enabled
        val isSpvEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false)
        if (!Session.getSession().isWatchOnly && isSpvEnabled) {
            try {
                spv.startService(context)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private val objectMapper = ObjectMapper()
    // This is needed for all GDK calls that returns a JSON
    // Prototype uses Kotlin Serialization for JSON decoding
    // This method will do the transition from JsonElement to ObjectNode if needed
    fun toJackson(obj: Any?): ObjectNode? {
        if(obj == null) return null
        
        return if (obj is JsonElement) {
            objectMapper.readTree(Json.encodeToString(obj)) as ObjectNode
        } else {
            obj as ObjectNode
        }
    }
}
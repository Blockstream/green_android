package com.greenaddress

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.blockstream.crypto.BuildConfig
import com.blockstream.gdk.AssetsProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HardwareQATester
import com.greenaddress.greenapi.Session
import com.greenaddress.greenbits.spv.SPV
import com.greenaddress.greenbits.ui.preferences.PrefKeys
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
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
    
    var isDevelopmentFlavor = false
        private set

    var versionName = "0.0.0"
        private set

    val spv = SPV()

    var hardwareQATester : HardwareQATester? = null

    var navigateFn: ((activity: FragmentActivity, type: NavigateType, gaSession: Any?, walletId: Long?) -> Unit)? = null
    var setSubaccountFn: ((gaSession: Any?, subaccount: Int) -> Unit)? = null
    var getSubaccountFn: ((gaSession: Any?) -> Int)? = null
    var getWalletNameFn: ((gaSession: Any?) -> String?)? = null
    var getActiveAssetProviderFn: ((gaSession: Any?) -> AssetsProvider?)? = null
    var getHWWalletFn: ((gaSession: Any?) -> HWWallet?)? = null
    var walletsProviderFn: ((gaSession: Any?) -> List<HashMap<String, String>>)? = null
    var recoveryConfirmedProviderFn: ((gaSession: Any?) -> Boolean)? = null
    var connectFn: ((context: Context, gaSession: Any?, networkId: String, hwWallet: HWWallet?) -> Unit)? = null
    var loginWithDeviceFn: ((context: Context, gaSession: Any?, networkId: String, connectSession: Boolean, hwWallet: HWWallet, hardwareDataResolver: HardwareCodeResolver) -> Unit)? = null

    private var initialized = false

    enum class NavigateType {
        LOGOUT, CHANGE_PIN, APP_SETTINGS, BACKUP_RECOVERY, TWO_FACTOR_RESET, ADD_ACCOUNT
    }

    fun initializeBridge(
        ctx: Context,
        isDevelopmentFlavor: Boolean,
        version: String,
        QATester : HardwareQATester
    ){
        if(!initialized) {
            initialized = true

            context = WeakReference(ctx.applicationContext)

            this.isDevelopmentFlavor = isDevelopmentFlavor
            versionName = version

            hardwareQATester = QATester

            Session.getSession().setDevelopmentFlavor(isDevelopmentFlavor)

            JadeAPI.isDebug = BuildConfig.DEBUG
        }
    }

    fun connect(context:Context, gaSession: Any?, network: String, hwWallet: HWWallet?){
        connectFn?.invoke(context, gaSession, network, hwWallet)
    }

    fun loginWithDevice(context:Context, gaSession: Any?, network: String,connectSession: Boolean,  hwWallet: HWWallet, hardwareDataResolver: HardwareCodeResolver){
        loginWithDeviceFn?.invoke(context, gaSession, network, connectSession, hwWallet, hardwareDataResolver)
    }

    fun getIsRecoveryConfirmed() = recoveryConfirmedProviderFn?.invoke(Session.getSession().nativeSession) ?: true

    fun getWallets() = walletsProviderFn?.invoke(Session.getSession().nativeSession)

    fun navigateToLogin(activity: FragmentActivity, walletId: Long? = null){
        navigateFn?.invoke(activity, NavigateType.LOGOUT, Session.getSession().nativeSession, walletId)
    }

    fun navigateToChangePin(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.CHANGE_PIN, Session.getSession().nativeSession, null)
    }

    fun navigateToBackupRecovery(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.BACKUP_RECOVERY, Session.getSession().nativeSession, null)
    }

    fun appSettingsDialog(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.APP_SETTINGS, Session.getSession().nativeSession, null)
    }

    fun twoFactorResetDialog(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_RESET, Session.getSession().nativeSession, null)
    }

    fun addAccount(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.ADD_ACCOUNT, Session.getSession().nativeSession, null)
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

    fun getWalletName(): String? {
        return getWalletNameFn?.let {

            val session = Session.getSession()

            if(session != null && session.nativeSession != null){
                session.nativeSession.let { nativeSession ->
                    return@getWalletName it.invoke(nativeSession)
                }
            }

            null
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

    fun getActiveAssetProvider(): AssetsProvider? {
        return getActiveAssetProviderFn?.let {
            val session = Session.getSession()

            if(session != null && session.nativeSession != null){
                session.nativeSession.let { nativeSession ->
                    return@getActiveAssetProvider it.invoke(nativeSession)
                }
            }

            null
        }
    }

    fun getHWWallet() = getHWWalletFn?.invoke(Session.getSession().nativeSession)

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
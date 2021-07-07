package com.greenaddress

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.blockstream.crypto.BuildConfig
import com.blockstream.gdk.AssetsProvider
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.TwoFactorResolver
import com.blockstream.gdk.data.Settings
import com.blockstream.gdk.data.SubAccount
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HardwareQATester
import com.greenaddress.greenapi.Session
import com.greenaddress.greenbits.ui.UI
import com.greenaddress.greenbits.ui.preferences.PrefKeys
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import com.greenaddress.jade.JadeAPI
import io.reactivex.Single
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

    var hardwareQATester : HardwareQATester? = null

    var navigateFn: ((activity: FragmentActivity, type: NavigateType, gaSession: Any?, extraData: Any?) -> Unit)? = null
    var setSubaccountFn: ((gaSession: Any?, subaccount: Int) -> Unit)? = null
    var updateSettingsV4Fn: ((gaSession: Any?) -> Unit)? = null
    var getSubaccountFn: ((gaSession: Any?) -> Int)? = null
    var getWalletNameFn: ((gaSession: Any?) -> String?)? = null
    var getWalletIdFn: ((gaSession: Any?) -> Long?)? = null
    var getActiveAssetProviderFn: ((gaSession: Any?) -> AssetsProvider?)? = null
    var getHWWalletFn: ((gaSession: Any?) -> HWWallet?)? = null
    var walletsProviderFn: ((gaSession: Any?) -> List<HashMap<String, String>>)? = null
    var sessionIsConnectedProviderFn: ((gaSession: Any?) -> Boolean)? = null
    var recoveryConfirmedProviderFn: ((gaSession: Any?) -> Boolean)? = null
    var connectFn: ((context: Context, gaSession: Any?, networkId: String, hwWallet: HWWallet?) -> Unit)? = null
    var loginWithDeviceFn: ((context: Context, gaSession: Any?, networkId: String, connectSession: Boolean, hwWallet: HWWallet, hardwareDataResolver: HardwareCodeResolver) -> Unit)? = null
    var createTwoFactorResolverFn: ((context: Context) -> TwoFactorResolver)? = null

    private var initialized = false

    enum class NavigateType {
        LOGOUT, CHANGE_PIN, SETTINGS, APP_SETTINGS, BACKUP_RECOVERY, TWO_FACTOR_RESET, ADD_ACCOUNT, RECEIVE, ACCOUNT_ID, TWO_FACTOR_CANCEL_RESET, TWO_FACTOR_DISPUTE, TWO_FACTOR_UNDO_DISPUTE, TWO_FACTOR_AUTHENTICATION
    }

    fun initializeBridge(
        ctx: Context,
        isDevelopmentFlavor: Boolean,
        applicationId: String,
        versionName: String,
        QATester : HardwareQATester,
        greenWallet: GreenWallet,
    ){
        if(!initialized) {
            initialized = true

            context = WeakReference(ctx.applicationContext)

            this.isDevelopmentFlavor = isDevelopmentFlavor
            this.versionName = versionName

            UI.PACKAGE_NAME = applicationId

            hardwareQATester = QATester

            Session.getSession().initFromV4(isDevelopmentFlavor, greenWallet)

            JadeAPI.isDebug = BuildConfig.DEBUG
        }
    }

    fun connect(context:Context, gaSession: Any?, network: String, hwWallet: HWWallet?){
        connectFn?.invoke(context, gaSession, network, hwWallet)
    }

    fun loginWithDevice(context:Context, gaSession: Any?, network: String,connectSession: Boolean,  hwWallet: HWWallet, hardwareDataResolver: HardwareCodeResolver){
        loginWithDeviceFn?.invoke(context, gaSession, network, connectSession, hwWallet, hardwareDataResolver)
    }

    fun isSessionConnected() = sessionIsConnectedProviderFn?.invoke(Session.getSession().nativeSession) ?: false

    fun getIsRecoveryConfirmed() = recoveryConfirmedProviderFn?.invoke(Session.getSession().nativeSession) ?: true

    fun getWallets() = walletsProviderFn?.invoke(Session.getSession().nativeSession)

    fun navigateToLogin(activity: FragmentActivity, walletId: Long? = null){
        navigateFn?.invoke(activity, NavigateType.LOGOUT, Session.getSession().nativeSession, walletId)
    }

    fun navigateToReceive(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.RECEIVE, Session.getSession().nativeSession, null)
    }

    fun navigateToChangePin(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.CHANGE_PIN, Session.getSession().nativeSession, null)
    }

    fun navigateToSettings(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.SETTINGS, Session.getSession().nativeSession, null)
    }

    fun navigateToBackupRecovery(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.BACKUP_RECOVERY, Session.getSession().nativeSession, null)
    }

    fun appSettingsDialog(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.APP_SETTINGS, Session.getSession().nativeSession, null)
    }

    fun accountIdDialog(activity: FragmentActivity, subAccount: SubAccount){
        navigateFn?.invoke(activity, NavigateType.ACCOUNT_ID, Session.getSession().nativeSession, subAccount)
    }

    fun twoFactorResetDialog(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_RESET, Session.getSession().nativeSession, null)
    }

    fun twoFactorAuthentication(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_AUTHENTICATION, Session.getSession().nativeSession, null)
    }

    fun twoFactorCancelReset(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_CANCEL_RESET, Session.getSession().nativeSession, null)
    }

    fun twoFactorUndoDisputeReset(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_UNDO_DISPUTE, Session.getSession().nativeSession, null)
    }

    fun twoFactorDisputeReset(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.TWO_FACTOR_DISPUTE, Session.getSession().nativeSession, null)
    }

    fun addAccount(activity: FragmentActivity){
        navigateFn?.invoke(activity, NavigateType.ADD_ACCOUNT, Session.getSession().nativeSession, null)
    }

    fun bridgeSession(session: Any, networkId: String, watchOnlyUsername: String?) {
        Session.getSession().bridgeSession(session, networkId, watchOnlyUsername)
        setCurrentNetwork(context.get()!!, networkId)
    }

    fun setActiveAccount(account : Int){
        setSubaccountFn?.invoke(Session.getSession().nativeSession, account)
    }

    // Send signal to v4 codebase to update settings data
    fun updateSettingsV4(){
        updateSettingsV4Fn?.invoke(Session.getSession().nativeSession)
    }

    // Send signal to v3 codebase to update settings data
    fun updateSettingsV3(){
        Session.getSession().refreshSettings()
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

    fun getActiveWalletId(): Long? {
        return getWalletIdFn?.let {

            val session = Session.getSession()

            if(session != null && session.nativeSession != null){
                session.nativeSession.let { nativeSession ->
                    return@getActiveWalletId it.invoke(nativeSession)
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

    fun createTwoFactorResolver(context: Context): TwoFactorResolver? = createTwoFactorResolverFn?.invoke(context)

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
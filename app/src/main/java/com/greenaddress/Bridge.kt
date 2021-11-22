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

    const val appModuleInUse = true
    
    var isDevelopmentFlavor = false
        private set

    var versionName = "0.0.0"
        private set

    var hardwareQATester : HardwareQATester? = null
    var greenWallet: GreenWallet? = null

    var navigateFn: ((activity: FragmentActivity, type: NavigateType, gaSession: Any?, extraData: Any?) -> Unit)? = null
    var getWalletIdFn: ((gaSession: Any?) -> Long?)? = null
    var getHWWalletFn: ((gaSession: Any?) -> HWWallet?)? = null
    var walletsProviderFn: ((gaSession: Any?) -> List<HashMap<String, String>>)? = null
    var sessionIsConnectedProviderFn: ((gaSession: Any?) -> Boolean)? = null
    var connectFn: ((context: Context, gaSession: Any?, networkId: String, hwWallet: HWWallet?) -> Unit)? = null
    var createTwoFactorResolverFn: ((context: Context) -> TwoFactorResolver)? = null

    private var initialized = false

    enum class NavigateType {
        LOGOUT, RECEIVE, TWO_FACTOR_CANCEL_RESET
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

            this.greenWallet = greenWallet

            this.isDevelopmentFlavor = isDevelopmentFlavor
            this.versionName = versionName

            UI.PACKAGE_NAME = applicationId

            hardwareQATester = QATester

            Session.getSession().initFromV4(greenWallet)

            JadeAPI.isDebug = BuildConfig.DEBUG
        }
    }

    fun connect(context:Context, gaSession: Any?, network: String, hwWallet: HWWallet?){
        connectFn?.invoke(context, gaSession, network, hwWallet)
    }

    fun isSessionConnected() = sessionIsConnectedProviderFn?.invoke(Session.getSession().nativeSession) ?: false

    fun navigateToLogin(activity: FragmentActivity, walletId: Long? = null){
        navigateFn?.invoke(activity, NavigateType.LOGOUT, Session.getSession().nativeSession, walletId)
    }

    fun bridgeSession(session: Any, networkId: String) {
        Session.getSession().bridgeSession(session, networkId)
        setCurrentNetwork(context.get()!!, networkId)
    }

    // Send signal to v3 codebase to update settings data
    fun updateSettingsV3(){
        Session.getSession().refreshSettings()
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

    fun getHWWallet() = getHWWalletFn?.invoke(Session.getSession().nativeSession)

    fun setCurrentNetwork(context: Context, networkId: String) = BridgeJava.setCurrentNetwork(
        context,
        networkId
    )

    fun getCurrentNetworkData(context: Context) = BridgeJava.getCurrentNetworkData(context)

    fun getCurrentNetworkId(context: Context) = BridgeJava.getCurrentNetwork(context)

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
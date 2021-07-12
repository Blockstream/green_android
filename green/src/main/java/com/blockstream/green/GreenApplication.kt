package com.blockstream.green

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDeepLinkBuilder
import com.blockstream.gdk.AssetManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.settings.Migrator
import com.blockstream.green.ui.BridgeActivity
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.QATesterActivity
import com.blockstream.green.ui.TwoFactorResetSheetDialogFragment
import com.blockstream.green.ui.receive.ReceiveFragmentArgs
import com.blockstream.green.ui.recovery.RecoveryIntroFragmentArgs
import com.blockstream.green.ui.settings.*
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.*
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.libgreenaddress.GASession
import com.greenaddress.Bridge
import com.greenaddress.Bridge.getActiveWalletId
import com.greenaddress.Bridge.navigateToLogin
import com.greenaddress.greenapi.Registry
import com.greenaddress.greenapi.Session
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


@HiltAndroidApp
class GreenApplication : Application(){

    @Inject
    lateinit var migrator: Migrator

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var assetManager: AssetManager

    @Inject
    lateinit var qaTester: QATester

    @Inject
    lateinit var greenWallet: GreenWallet

    override fun onCreate() {
        super.onCreate()

        // Initialize Bridge
        Bridge.initializeBridge(this, isDevelopmentFlavor(), BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, qaTester, greenWallet)

        Bridge.navigateFn = { activity: FragmentActivity, type: Bridge.NavigateType, gaSession: GASession?, extraData: Any? ->
            when(type){
                Bridge.NavigateType.LOGOUT -> {

                    // Disconnect session
                    gaSession?.let {
                        sessionManager.getWalletSession(it)?.disconnectAsync()
                    }

                    // Navigate to MainActivity
                    NavDeepLinkBuilder(activity.applicationContext)
                        .setGraph(R.navigation.nav_graph)
                        .setComponentName(MainActivity::class.java)
                        .setDestination(R.id.introFragment).also { builder ->
                            // Navigate to Login
                            if(extraData is Long){
                                extraData.let { navigateToWallet ->
                                    val walletId =
                                        if (navigateToWallet == -1L) sessionManager.getWalletIdFromSession(
                                            gaSession
                                        ) else navigateToWallet

                                    walletRepository.getWalletSync(walletId)?.let {
                                        builder.setDestination(R.id.loginFragment)
                                        builder.setArguments(LoginFragmentArgs(it).toBundle())
                                    }
                                }
                            }
                        }
                        .createPendingIntent()
                        .send()
                }

                Bridge.NavigateType.CHANGE_PIN, Bridge.NavigateType.SETTINGS -> {
                    sessionManager.getWalletSession(gaSession)?.let { session ->
                        GlobalScope.launch {
                            val wallet = getWalletOrEmulatedHardwareWallet(gaSession, session.network)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            intent.putExtras(WalletSettingsFragmentArgs(wallet, type == Bridge.NavigateType.CHANGE_PIN).toBundle())
                            intent.action = BridgeActivity.PIN

                            activity.startActivity(intent)
                        }
                    }
                }

                Bridge.NavigateType.BACKUP_RECOVERY -> {

                    val walletId = sessionManager.getWalletIdFromSession(gaSession)

                    if (walletId >= 0) {

                        GlobalScope.launch {
                            val wallet = walletRepository.getWalletSuspend(walletId)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            // Seems there is a bug in Default Nav args, so we must set a dummy OnboardingOptions
                            intent.putExtras(
                                RecoveryIntroFragmentArgs(
                                    wallet, onboardingOptions = OnboardingOptions(
                                        false
                                    )
                                ).toBundle()
                            )
                            intent.action = BridgeActivity.BACKUP_RECOVERY

                            activity.startActivity(intent)

                        }
                    }
                }

                Bridge.NavigateType.APP_SETTINGS -> {
                    AppSettingsDialogFragment().also {
                        it.show(activity.supportFragmentManager, it.toString())
                    }
                }

                Bridge.NavigateType.ACCOUNT_ID -> {
                    if(extraData is SubAccount) {
                        AccountIdBottomSheetDialogFragment(extraData).also {
                            it.show(activity.supportFragmentManager, it.toString())
                        }
                    }
                }

                Bridge.NavigateType.TWO_FACTOR_RESET -> {
                    sessionManager.getWalletSession(gaSession)?.getTwoFactorResetObservable()
                        ?.blockingFirst()?.let {
                        TwoFactorResetSheetDialogFragment.newInstance(it).also { dialog ->
                            dialog.show(activity.supportFragmentManager, dialog.toString())
                        }
                    }
                }

                Bridge.NavigateType.TWO_FACTOR_AUTHENTICATION,
                Bridge.NavigateType.TWO_FACTOR_CANCEL_RESET,
                Bridge.NavigateType.TWO_FACTOR_DISPUTE,
                Bridge.NavigateType.TWO_FACTOR_UNDO_DISPUTE -> {
                    sessionManager.getWalletSession(gaSession)?.let { session ->

                        GlobalScope.launch {

                            val wallet = getWalletOrEmulatedHardwareWallet(gaSession, session.network)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            if(type == Bridge.NavigateType.TWO_FACTOR_AUTHENTICATION){
                                intent.putExtras(
                                    WalletSettingsFragmentArgs(
                                        wallet = wallet,
                                        bridgeTwoFactorAuthentication = true,
                                    ).toBundle()
                                )
                                intent.action = BridgeActivity.TWO_FACTOR_AUTHENTICATION
                            }else {
                                intent.putExtras(
                                    WalletSettingsFragmentArgs(
                                        wallet = wallet,
                                        bridgeTwoFactorReset = true,
                                        bridgeTwoFactorSetupType = when (type) {
                                            Bridge.NavigateType.TWO_FACTOR_CANCEL_RESET -> TwoFactorSetupAction.CANCEL
                                            Bridge.NavigateType.TWO_FACTOR_DISPUTE -> TwoFactorSetupAction.DISPUTE
                                            else -> TwoFactorSetupAction.UNDO_DISPUTE
                                        }
                                    ).toBundle()
                                )
                                intent.action = BridgeActivity.TWO_FACTOR_RESET
                            }

                            activity.startActivity(intent)
                        }
                    }
                }

                Bridge.NavigateType.ADD_ACCOUNT -> {
                    sessionManager.getWalletSession(gaSession)?.let { session ->

                        GlobalScope.launch {

                            val wallet = getWalletOrEmulatedHardwareWallet(gaSession, session.network)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            intent.putExtras(ChooseAccountTypeFragmentArgs(wallet).toBundle())
                            intent.action = BridgeActivity.ADD_ACCOUNT

                            activity.startActivity(intent)
                        }

                    }
                }

                Bridge.NavigateType.RECEIVE -> {
                    sessionManager.getWalletSession(gaSession)?.let { session ->

                        GlobalScope.launch {
                            val wallet = getWalletOrEmulatedHardwareWallet(gaSession, session.network)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            intent.putExtras(ReceiveFragmentArgs(wallet).toBundle())
                            intent.action = BridgeActivity.RECEIVE

                            activity.startActivity(intent)
                        }

                    }
                }
            }
        }

        Bridge.setSubaccountFn = { gaSession, subaccount ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                walletRepository.getWalletSync(walletId)?.let {
                    it.activeAccount = subaccount.toLong()
                    walletRepository.updateWalletSync(it)
                }
            }

            Session.getSession().subAccount = subaccount
        }

        Bridge.updateSettingsV4Fn = { gaSession ->
            sessionManager.getWalletSession(gaSession)?.updateSettings()
        }

        Bridge.walletsProviderFn = { gaSession ->

            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            val wallets = mutableListOf<HashMap<String, String>>()
            for(wallet in walletRepository.getWalletsSync()){
                val w = hashMapOf<String, String>()
                w["id"] = wallet.id.toString()
                w["name"] = wallet.name
                w["network"] = wallet.network
                w["active"] = if(walletId == wallet.id) "true" else "false"

                wallets.add(w)
            }

            wallets
        }

        Bridge.recoveryConfirmedProviderFn = { gaSession ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                walletRepository.getWalletSync(walletId)?.isRecoveryPhraseConfirmed ?: true
            }else{
                true
            }
        }

        Bridge.sessionIsConnectedProviderFn = { gaSession ->
            sessionManager.getWalletSession(gaSession)?.isConnected ?: false
        }

        Bridge.getSubaccountFn = { gaSession ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                 walletRepository.getWalletSync(walletId)?.activeAccount?.toInt() ?: 0
            }else{
                Session.getSession()?.subAccount ?: 0
            }
        }

        Bridge.getWalletNameFn = { gaSession ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                walletRepository.getWalletSync(walletId)?.name
            }else{
                null
            }
        }

        Bridge.getWalletIdFn = { gaSession ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                walletRepository.getWalletSync(walletId)?.id
            }else{
                null
            }
        }

        Bridge.getActiveAssetProviderFn = { gaSession ->
            sessionManager.getWalletSession(gaSession)
        }

        Bridge.connectFn = { _, gaSession, networkId, hwWallet->
            sessionManager.getWalletSession(gaSession)?.let {
                it.connect(it.networks.getNetworkById(networkId), hwWallet)
            }
        }

        Bridge.loginWithDeviceFn = { _, gaSession, networkId, connectSession, hwWallet, hardwareDataResolver ->
            sessionManager.getWalletSession(gaSession)?.let {
                it.loginWithDevice(it.networks.getNetworkById(networkId),
                    registerUser = true,
                    connectSession = connectSession,
                    hwWallet = hwWallet,
                    hardwareCodeResolver = hardwareDataResolver
                )
            }
        }

        Bridge.getHWWalletFn = { gaSession ->
            gaSession?.let {
                return@let sessionManager.getWalletSession(it)?.hwWallet
            }
        }

        // Init Registry
        Registry.init(assetManager)

        GlobalScope.launch(Dispatchers.IO){
            migrator.migrate()
        }

        if (isDevelopmentFlavor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            initShortcuts()
        }
    }

    private suspend fun getWalletOrEmulatedHardwareWallet(gaSession: GASession?, network: Network): Wallet {
        val walletId = sessionManager.getWalletIdFromSession(gaSession)

        return if (walletId >= 0) {
            walletRepository.getWalletSuspend(walletId)
        } else {
            // Emulated Hardware wallet
            Wallet(
                id = -1L,
                name = network.name,
                network = network.network,
                isRecoveryPhraseConfirmed = true,
                isHardware = true,
                activeAccount = Session.getSession().subAccount.toLong()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun initShortcuts(){
        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val shortcut = ShortcutInfo.Builder(this, "QATester")
            .setShortLabel("QA Tester")
            .setLongLabel("QA Tester")
            .setIcon(Icon.createWithResource(this, R.drawable.blockstream_jade_device))
            .setIntent(Intent(Intent.ACTION_VIEW, null, this, QATesterActivity::class.java))
            .build()
        shortcutManager!!.dynamicShortcuts = listOf(shortcut)
    }
}
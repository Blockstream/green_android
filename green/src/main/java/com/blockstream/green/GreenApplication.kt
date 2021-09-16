package com.blockstream.green

import android.app.Application
import android.content.Context
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
import com.greenaddress.greenapi.Registry
import com.pandulapeter.beagle.Beagle
import dagger.hilt.android.HiltAndroidApp
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

    @Inject
    lateinit var beagle: Beagle

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

        Bridge.sessionIsConnectedProviderFn = { gaSession ->
            sessionManager.getWalletSession(gaSession)?.isConnected ?: false
        }

        Bridge.getSubaccountFn = { gaSession ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                 walletRepository.getWalletSync(walletId)?.activeAccount?.toInt() ?: 0
            }else{
                // On HHW activeAccount is saved on the Wallet object (but not on the DB) and into the GreenSession
                sessionManager.getWalletSession(gaSession)?.activeAccount?.toInt() ?: 0
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

        Bridge.createTwoFactorResolverFn = { context: Context ->
            DialogTwoFactorResolver(context)
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
                walletHashId = network.id,
                name = network.name,
                network = network.network,
                isRecoveryPhraseConfirmed = true,
                isHardware = true,
                activeAccount = 0
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
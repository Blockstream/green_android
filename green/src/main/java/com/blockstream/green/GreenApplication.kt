package com.blockstream.green

import android.app.Application
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDeepLinkBuilder
import com.blockstream.green.settings.Migrator
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.lifecycle.AppLifecycleObserver
import com.blockstream.green.ui.BridgeActivity
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.ui.TwoFactorResetSheetDialogFragment
import com.blockstream.green.ui.wallet.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.LoginFragmentArgs
import com.blockstream.green.ui.recovery.RecoveryIntroFragmentArgs
import com.blockstream.green.ui.settings.WalletSettingsFragmentArgs
import com.blockstream.libgreenaddress.GASession
import com.greenaddress.Bridge
import com.greenaddress.greenapi.Session
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    // Inject it just to be initialized
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        // Initialize Bridge
        Bridge.initializeBridge(this, BuildConfig.DEBUG, BuildConfig.VERSION_NAME)

        Bridge.setNavigateHandler { activity: FragmentActivity, type: Bridge.NavigateType, gaSession: Any? ->
            when(type){
                Bridge.NavigateType.LOGOUT -> {

                    // Disconnect session
                    gaSession?.let {
                        sessionManager.getWalletSession(it)?.disconnectAsync()
                    }

                    // Replace Activity
                    val intent = Intent(activity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }

                Bridge.NavigateType.CHANGE_PIN -> {

                    val walletId = sessionManager.getWalletIdFromSession(gaSession)

                    if (walletId >= 0) {

                        GlobalScope.launch {
                            val wallet = walletRepository.getWalletSuspend(walletId)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            intent.putExtras(WalletSettingsFragmentArgs(wallet, true).toBundle())
                            intent.action = BridgeActivity.PIN

                            activity.startActivity(intent)
                        }
                    }
                }

                Bridge.NavigateType.BACKUP_RECOVERY -> {

                    val walletId = sessionManager.getWalletIdFromSession(gaSession)

                    if(walletId >= 0){

                        GlobalScope.launch {
                            val wallet = walletRepository.getWalletSuspend(walletId)

                            val intent = Intent(activity, BridgeActivity::class.java)
                            // Seems there is a bug in Default Nav args, so we must set a dummy OnboardingOptions
                            intent.putExtras(RecoveryIntroFragmentArgs(wallet, onboardingOptions = OnboardingOptions(false)).toBundle())
                            intent.action = BridgeActivity.BACKUP_RECOVERY

                            activity.startActivity(intent)

                        }
                    }
                }

                Bridge.NavigateType.CONNECTION_SETTINGS -> {
                    DeleteWalletBottomSheetDialogFragment().also {
                        it.show(activity.supportFragmentManager, it.toString())
                    }
                }

                Bridge.NavigateType.TWO_FACTOR_RESET -> {
                    sessionManager.getWalletSession(gaSession)?.getTwoFactorResetObservable()?.blockingFirst()?.let {
                        TwoFactorResetSheetDialogFragment.newInstance(it).also { dialog ->
                            dialog.show(activity.supportFragmentManager, dialog.toString())
                        }
                    }
                }
            }
        }

        Bridge.setActiveAccountHandler { gaSession, subaccount ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){

                walletRepository.getWalletSync(walletId)?.let {
                    it.activeAccount = subaccount.toLong()
                    walletRepository.updateWalletSync(it)
                }
            }
        }

        Bridge.setWalletProvider { gaSession ->

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

        Bridge.setRecoveryConfirmedProvider { gaSession ->

            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                return@setRecoveryConfirmedProvider walletRepository.getWalletSync(walletId)?.isRecoveryPhraseConfirmed ?: true
            }

            true
        }


        Bridge.setGetActiveAccountHandler { gaSession ->
            val walletId = sessionManager.getWalletIdFromSession(gaSession)

            if(walletId >= 0){
                 walletRepository.getWalletSync(walletId)?.let {
                     return@setGetActiveAccountHandler it.activeAccount.toInt()
                }
            }

            0
        }

        Bridge.setConnectHandler { context, gaSession, networkId ->

            sessionManager.getWalletSession(gaSession)?.let {
                it.connect(it.networks.getNetworkById(networkId))

                try {
                    Bridge.spv.startService(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

        GlobalScope.launch(Dispatchers.IO){
            migrator.migrate()
        }
    }
}
package com.blockstream.green.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.OnRebindCallback
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.transition.TransitionManager
import com.blockstream.green.R
import com.blockstream.green.databinding.MainActivityBinding
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.ui.devices.DeviceInfoBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.LoginFragment
import com.blockstream.green.utils.*
import com.blockstream.green.views.GreenToolbar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var appKeystore: AppKeystore

    private var unlockPrompt: BiometricPrompt? = null

    private lateinit var binding: MainActivityBinding
    private val activityViewModel: MainActivityViewModel by viewModels()

    override val toolbar: GreenToolbar
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isDevelopmentFlavor()) {
            // On development flavor, you can change settings using intent data, useful for UI tests
            intent?.let {
                if (it.hasExtra(ENABLE_TESTNET)) {
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            testnet = it.getBooleanExtra(ENABLE_TESTNET, false)
                        )
                    )
                }

                if (it.hasExtra(ENABLE_TOR)) {
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            tor = it.getBooleanExtra(ENABLE_TOR, false)
                        )
                    )
                }

                if (it.hasExtra(PROXY_URL)) {
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            proxyUrl = it.getStringExtra(PROXY_URL)
                        )
                    )
                }

                if (it.hasExtra(PERSONAL_ELECTRUM_SERVER)) {
                    val electrumServer = it.getStringExtra(PERSONAL_ELECTRUM_SERVER);
                    settingsManager.saveApplicationSettings(
                        settingsManager.getApplicationSettings().copy(
                            electrumNode = !electrumServer.isNullOrBlank(),
                            personalBitcoinElectrumServer = electrumServer,
                            personalLiquidElectrumServer = electrumServer,
                            personalTestnetElectrumServer = electrumServer,
                            personalTestnetLiquidElectrumServer = electrumServer,
                        )
                    )
                }
            }
        }

        binding = MainActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.vm = activityViewModel

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.also {
            // Prevent replacing title from NavController
            it.setDisplayShowTitleEnabled(false)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.overviewFragment, R.id.loginFragment, R.id.introFragment),
            binding.drawerLayout
        )

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        binding.toolbar.setLogoClickListener {
            navHostFragment.childFragmentManager.fragments.firstOrNull()?.let { fragment ->
                // Except LoginFragment as we don't want concurrent login requests to happen
                if(fragment is WalletFragment<*> && fragment !is LoginFragment){
                    fragment.session.device?.let {
                        DeviceInfoBottomSheetDialogFragment.create(it.id).also {
                            it.show(navHostFragment.childFragmentManager, it.toString())
                        }
                    }
                }
            }
        }

        binding.buttonUnlock.setOnClickListener {
            showUnlockPrompt()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.appBarLayout.isInvisible =
                (destination.id == R.id.introFragment || destination.id == R.id.onBoardingCompleteFragment)
        }

        // Set version into the main VM
        activityViewModel.buildVersion.value =
            getString(R.string.id_version_1s_2s).format(getVersionName(this), "")

        setupSecureScreenListener()

        handleIntent(intent)

        // Animate Screen Lock
        // https://medium.com/androiddevelopers/android-data-binding-animations-55f6b5956a64
        binding.addOnRebindCallback(object : OnRebindCallback<MainActivityBinding>() {
            override fun onPreBind(binding: MainActivityBinding): Boolean {
                // Animate only when unlocking as we want immediate hiding of contents
                if(activityViewModel.lockScreen.value == false) {
                    TransitionManager.beginDelayedTransition((binding.root as ViewGroup))
                }
                return super.onPreBind(binding)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if(activityViewModel.lockScreen.value == true && unlockPrompt == null){
            // check unlockPrompt if there is already a prompt displayed
            showUnlockPrompt()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun showUnlockPrompt(){
        unlockPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_unlock_green))
            .setConfirmationRequired(false)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }

        unlockPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : AuthenticationCallback(this) {
                override fun onAuthenticationFailed() {
                    unlockPrompt = null
                    super.onAuthenticationFailed()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    unlockPrompt = null
                    if(errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL){
                        // User hasn't enabled any device credential,
                        activityViewModel.unlock()
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockPrompt = null
                    activityViewModel.unlock()
                }
            })

        try {
            // Ask for user presence
            unlockPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            e.printStackTrace()
            unlockPrompt = null
            activityViewModel.unlock()
        }
    }

    private fun handleIntent(intent: Intent?) {
        // Handle BIP-21 uri
        intent?.data?.let {
            sessionManager.pendingBip21Uri.postValue(ConsumableEvent(it.toString()))

            if(navController.currentDestination?.id == R.id.introFragment) {
                Snackbar.make(
                    binding.root,
                    R.string.id_you_have_clicked_a_payment_uri,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.id_cancel) {
                    sessionManager.pendingBip21Uri.postValue(null)
                }.show()
            }
        }
    }

    override fun isDrawerOpen(): Boolean = binding.drawerLayout.isDrawerOpen(GravityCompat.START)

    override fun lockDrawer(isLocked: Boolean) {
        binding.drawerLayout.setDrawerLockMode(if (isLocked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun closeDrawer() {
        binding.drawerLayout.closeDrawers()
    }

    override fun setToolbarVisibility(isVisible: Boolean) {
        binding.appBarLayout.isVisible = isVisible
    }

    override fun onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    companion object: KLogging() {
        const val ENABLE_TESTNET = "ENABLE_TESTNET"
        const val ENABLE_TOR = "ENABLE_TOR"
        const val PROXY_URL = "PROXY_URL"
        const val PERSONAL_ELECTRUM_SERVER = "PERSONAL_ELECTRUM_SERVER"
    }
}
package com.blockstream.green.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Base64
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.transition.TransitionManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.PinData
import com.blockstream.green.ApplicationScope
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.data.ScreenView
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.MainActivityBinding
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.ui.devices.DeviceInfoBottomSheetDialogFragment
import com.blockstream.green.ui.dialogs.CountlyNpsDialogFragment
import com.blockstream.green.ui.dialogs.CountlySurveyDialogFragment
import com.blockstream.green.ui.wallet.LoginFragment
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.AuthenticationCallback
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.getVersionName
import com.blockstream.green.utils.handleException
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.navigate
import com.blockstream.green.views.GreenToolbar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ly.count.android.sdk.ModuleFeedback
import mu.KLogging
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@AndroidEntryPoint
class MainActivity : AppActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var appKeystore: AppKeystore

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var greenWallet: GreenWallet

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var countly: Countly

    private var unlockPrompt: BiometricPrompt? = null

    private lateinit var binding: MainActivityBinding
    private val activityViewModel: MainActivityViewModel by viewModels()

    override val toolbar: GreenToolbar
        get() = binding.toolbar

    val navHostFragment: NavHostFragment
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isDevelopmentFlavor) {
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

                if (it.hasExtra(REGISTER_NETWORK_ID) && it.hasExtra(REGISTER_NETWORK_HOSTNAME)) {
                    greenWallet.registerCustomNetwork(it.getStringExtra(REGISTER_NETWORK_ID) ?: "", it.getStringExtra(REGISTER_NETWORK_HOSTNAME) ?: "")
                }

                if (it.hasExtra(ADD_WALLET)) {
                    lifecycleScope.launch(context = handleException()) {
                        val json = Json.parseToJsonElement(
                            String(
                                Base64.decode(
                                    it.getStringExtra(ADD_WALLET)!!,
                                    Base64.DEFAULT
                                )!!
                            )
                        )

                        val walletJson = json.jsonObject["wallet"]!!.jsonObject
                        val network = walletJson["network"]!!.jsonPrimitive.content
                        val name = walletJson["name"]?.jsonPrimitive?.content

                        network.replaceFirstChar { it.titlecase() }

                        val wallet = Wallet(
                            walletHashId = "",
                            name = name ?: network.replaceFirstChar { n -> n.titlecase() },
                            network = network
                        )

                        val walletId = walletRepository.addWalletSuspend(wallet)

                        json.jsonObject["login_credentials"]?.jsonObject?.let { loginCredentialsJson ->

                            val pinData = loginCredentialsJson["pin_data"]!!.jsonObject.let {
                                PinData(
                                    encryptedData = it["encrypted_data"]!!.jsonPrimitive.content,
                                    pinIdentifier = it["pin_identifier"]!!.jsonPrimitive.content,
                                    salt = it["salt"]!!.jsonPrimitive.content,
                                )
                            }

                            walletRepository.addLoginCredentialsSuspend(
                                LoginCredentials(
                                    walletId = walletId,
                                    credentialType = CredentialType.PIN,
                                    pinData = pinData
                                )
                            )
                        }
                    }
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

        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.overviewFragment, R.id.loginFragment, R.id.introFragment),
            binding.drawerLayout
        )

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        binding.toolbar.setLogoClickListener {
            getVisibleFragment()?.let { fragment ->
                // Except LoginFragment as we don't want concurrent login requests to happen
                if(fragment is WalletFragment<*> && fragment !is LoginFragment){
                    fragment.session.device?.let {
                        DeviceInfoBottomSheetDialogFragment.show(it.id, navHostFragment.childFragmentManager)
                    }
                }
            }
        }

        binding.buttonUnlock.setOnClickListener {
            showUnlockPrompt()
        }

        combine(countly.feedbackWidgetStateFlow, navController.currentBackStackEntryFlow) { feedback, currentBackStackEntry ->
             (feedback != null && currentBackStackEntry.destination.id == R.id.overviewFragment)
        }.distinctUntilChanged().onEach {showDialog ->
            if(showDialog) {
                lifecycleScope.launchWhenResumed {
                    // Delay a bit
                    delay(1.toDuration(DurationUnit.SECONDS))

                    countly.feedbackWidget?.type.also { type ->
                        if(type == ModuleFeedback.FeedbackWidgetType.nps){
                            CountlyNpsDialogFragment.show(supportFragmentManager)
                        }else if(type == ModuleFeedback.FeedbackWidgetType.survey){
                            CountlySurveyDialogFragment.show(supportFragmentManager)
                        }
                    }
                }
            }
        }.launchIn(lifecycleScope)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            getVisibleFragment()?.let {
                if(it is ScreenView){
                    // Mark it as not recorded
                    it.screenIsRecorded = false
                }
            }

            binding.appBarLayout.isInvisible =
                (destination.id == R.id.onBoardingCompleteFragment)
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

    override fun onStart() {
        super.onStart()
        countly.onStart(this)
    }

    override fun onStop() {
        super.onStop()
        countly.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        countly.onConfigurationChanged(newConfig)
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

        if(intent?.action == OPEN_WALLET){
            intent.getParcelableExtra<Wallet>(WALLET)?.let { wallet ->
                NavGraphDirections.actionGlobalLoginFragment(
                    wallet = wallet,
                    deviceId = intent.getStringExtra(DEVICE_ID)
                ).let {
                    navigate(navController, resId = it.actionId, args = it.arguments)
                }
            }
            closeDrawer()
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

    private fun getVisibleFragment() = navHostFragment.childFragmentManager.fragments.firstOrNull()

    companion object: KLogging() {
        const val OPEN_WALLET = "OPEN_WALLET"
        const val WALLET = "WALLET"
        const val DEVICE_ID = "DEVICE_ID"

        const val ENABLE_TESTNET = "ENABLE_TESTNET"
        const val ENABLE_TOR = "ENABLE_TOR"
        const val PROXY_URL = "PROXY_URL"
        const val PERSONAL_ELECTRUM_SERVER = "PERSONAL_ELECTRUM_SERVER"

        const val REGISTER_NETWORK_ID = "REGISTER_NETWORK_ID"
        const val REGISTER_NETWORK_HOSTNAME = "REGISTER_NETWORK_HOSTNAME"

        const val ADD_WALLET = "ADD_WALLET"
    }
}
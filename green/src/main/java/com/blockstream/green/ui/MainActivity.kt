package com.blockstream.green.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.blockstream.green.R
import com.blockstream.green.databinding.MainActivityBinding
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.AuthenticationCallback
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.getVersionName
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppActivity() {

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var appKeystore: AppKeystore

    private var unlockPrompt: BiometricPrompt? = null

    private lateinit var binding: MainActivityBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.vm = viewModel

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

        binding.buttonUnlock.setOnClickListener {
            showUnlockPrompt()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.appBarLayout.isInvisible =
                (destination.id == R.id.introFragment || destination.id == R.id.onBoardingCompleteFragment)
        }

        // Set version into the main VM
        viewModel.buildVersion.value =
            getString(R.string.id_version_1s_2s).format(getVersionName(this), "")

        setupSecureScreenListener()

        handleIntent(intent)

        viewModel.lockScreen.value = canLock()

        viewModel.lockScreen.observe(this){ isLocked ->
            if(isLocked){
                showUnlockPrompt()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onPause() {
        super.onPause()

        if(canLock()){
            lifecycleScope.launchWhenResumed {
                viewModel.lockScreen.value = true
            }
        }
    }

    private fun canLock() = settingsManager.getApplicationSettings().enhancedPrivacy && appKeystore.canUseBiometrics(this)

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
                        viewModel.lockScreen.value = false
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockPrompt = null
                    viewModel.lockScreen.value = false
                }
            })

        try {
            // Ask for user presence
            unlockPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            e.printStackTrace()
            unlockPrompt = null
            viewModel.lockScreen.value = false
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

    override fun setToolbar(
        title: String?, subtitle: String?, drawable: Drawable?, button: CharSequence?,
        buttonListener: View.OnClickListener?
    ){
        binding.toolbar.set(title, subtitle, drawable, null, button, buttonListener)
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

    companion object: KLogging()
}
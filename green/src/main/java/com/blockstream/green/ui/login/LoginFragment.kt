package com.blockstream.green.ui.login

import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.data
import com.blockstream.common.data.isEmpty
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.LoginFragmentBinding
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.extensions.authenticateWithBiometrics
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.errorSnackbar
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.ui.bottomsheets.Bip39PassphraseBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.GreenAlertView
import com.blockstream.green.views.GreenPinViewListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.security.UnrecoverableKeyException

class LoginFragment : AppFragment<LoginFragmentBinding>(
    layout = R.layout.login_fragment,
    menuRes = R.menu.login
) {
    val args: LoginFragmentArgs by navArgs()
    val device by lazy { deviceManager.getDevice(args.deviceId) }

    val viewModel: LoginViewModel by viewModel { parametersOf(args.wallet, args.isLightningShortcut, args.autoLoginWallet, device) }

    private val appKeystore: AppKeystore by inject()

    private val deviceManager: DeviceManager by inject()

    private var biometricPrompt : BiometricPrompt? = null

    override val screenName = "Login"

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override val title: String
        get() = viewModel.greenWallet.name

    override val subtitle: String?
        get() = if(args.isLightningShortcut) getString(R.string.id_lightning_account) else null

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        when (sideEffect) {
            is SideEffects.WalletDelete -> {
                NavGraphDirections.actionGlobalHomeFragment().let { directions ->
                    navigate(directions.actionId, directions.arguments, isLogout = true)
                }
            }

            is SideEffects.Navigate -> {
                (sideEffect.data as? GreenWallet)?.also {
                    logger.info { "Login successful" }
                    navigate(LoginFragmentDirections.actionGlobalWalletOverviewFragment(wallet = sideEffect.data as GreenWallet))
                }

                (sideEffect.data as? Credentials)?.also {
                    logger.info { "Emergency Recovery Phrase" }
                    navigate(
                        LoginFragmentDirections.actionGlobalRecoveryPhraseFragment(
                            wallet = null,
                            credentials = sideEffect.data as Credentials
                        )
                    )
                }
            }

            is LoginViewModel.LocalSideEffects.PinError -> {
                binding.pinView.reset(true)
            }

            is LoginViewModel.LocalSideEffects.LaunchBiometrics -> {
                launchBiometricPrompt(sideEffect.loginCredentials)
            }

            is LoginViewModel.LocalSideEffects.AskBip39Passphrase -> {
                Bip39PassphraseBottomSheetDialogFragment.show(childFragmentManager)
            }

            is LoginViewModel.LocalSideEffects.LaunchUserPresenceForLightning -> {
                launchUserPresencePromptForLightningShortcut()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        viewModel.walletName.onEach {
            updateToolbar()
        }.launchIn(lifecycleScope)

        viewModel.pinCredentials.onEach { dataState ->
            dataState.data()?.also {

                if(it.counter > 0){
                    val errorMessage = if(it.counter == 2L) {
                        getString(R.string.id_last_attempt_if_failed_you_will)
                    }else {
                        getString(R.string.id_invalid_pin_you_have_1d, 3 - it.counter)
                    }

                    binding.pinView.setError(errorMessage)
                }
            }

            invalidateMenu()
        }.launchIn(lifecycleScope)

        viewModel.passwordCredentials.onEach { dataState ->
            dataState.data()?.also {
                if(it.counter > 0){
                    val errorMessage = if(it.counter == 2L) {
                        getString(R.string.id_last_attempt_if_failed_you_will)
                    }else {
                        getString(R.string.id_invalid_pin_you_have_1d, 3 - it.counter)
                    }

                    binding.passwordLayout.error = errorMessage
                }
            }

            invalidateMenu()
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.buttonLoginWithBiometrics.setOnClickListener {
            viewModel.biometricsCredentials.data()?.let {
                launchBiometricPrompt(it)
            }
        }

        binding.iconLightningShortcut.setOnClickListener {
             viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(false))
        }

        binding.pinView.listener = object : GreenPinViewListener {
            override fun onPin(pin: String) {
                viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithPin(pin))
            }

            override fun onPinChange(pinLength: Int, intermediatePin: String?) {}

            override fun onPinNotVerified() {}

            override fun onChangeMode(isVerify: Boolean) {}
        }

        binding.buttonRestoreWallet.setOnClickListener {
            navigate(
                LoginFragmentDirections.actionLoginFragmentToEnterRecoveryPhraseFragment(
                    setupArgs = SetupArgs.restoreMnemonic(
                        greenWallet = viewModel.greenWallet
                    ),
                )
            )
        }

        binding.buttonWatchOnlyLogin.setOnClickListener {
            viewModel.postEvent(LoginViewModel.LocalEvents.LoginWatchOnly)
        }

        binding.buttonLoginWithPassword.setOnClickListener {
            viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithPin(binding.password ?: ""))
            hideKeyboard()
        }

        binding.buttonLoginWithDevice.setOnClickListener {
            viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithDevice)
        }

        binding.passphraseButton.setOnClickListener {
            Bip39PassphraseBottomSheetDialogFragment.show(childFragmentManager)
        }

        binding.buttonEmergencyRecoveryPhrase.setOnClickListener {
            viewModel.postEvent(LoginViewModel.LocalEvents.EmergencyRecovery(false))
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

        menu.findItem(R.id.help).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware && !viewModel.greenWallet.isWatchOnly && viewModel.pinCredentials.isEmpty() && viewModel.passwordCredentials.isEmpty()
        menu.findItem(R.id.bip39_passphrase).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware && !viewModel.greenWallet.isWatchOnly
        menu.findItem(R.id.rename).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware
        menu.findItem(R.id.delete).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware
        menu.findItem(R.id.show_recovery_phrase).isVisible = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware && !viewModel.greenWallet.isWatchOnly
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.bip39_passphrase -> {
                Bip39PassphraseBottomSheetDialogFragment.show(childFragmentManager)
            }
            R.id.delete -> {
                DeleteWalletBottomSheetDialogFragment.show(viewModel.greenWallet, childFragmentManager)
            }
            R.id.rename -> {
                RenameWalletBottomSheetDialogFragment.show(viewModel.greenWallet, childFragmentManager)
            }
            R.id.show_recovery_phrase -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_emergency_recovery_phrase)
                    .setMessage(R.string.id_if_for_any_reason_you_cant)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.postEvent(LoginViewModel.LocalEvents.EmergencyRecovery(true))
                    }
                    .show()
            }
            R.id.help -> {
                openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_MNEMONIC_BACKUP)
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    private fun launchBiometricPrompt(loginCredentials: LoginCredentials, onlyDeviceCredentials: Boolean = false) {
        biometricPrompt?.cancelAuthentication()

        val isV4Authentication = loginCredentials.keystore.isNullOrBlank()

        if(isV4Authentication && appKeystore.isBiometricsAuthenticationRequired()){
            authenticateWithBiometrics(object : AuthenticationCallback(fragment = this) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // authenticateUserIfRequired = false prevent eternal loops
                    launchBiometricPrompt(loginCredentials = loginCredentials, onlyDeviceCredentials = true)
                }
            }, onlyDeviceCredentials = onlyDeviceCredentials)
            return
        }

        loginCredentials.encrypted_data?.let { encryptedData ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.id_login_with_biometrics))
                .setConfirmationRequired(true)

            if(isV4Authentication){
                promptInfo
                    .setNegativeButtonText(getString(R.string.id_cancel))
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
            }else{
                // V3 only needs user presence
                // Valid combinations for each SDK
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    promptInfo.setAllowedAuthenticators(BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                } else {
                    promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
            }

            biometricPrompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(requireContext()),
                object : AuthenticationCallback(this) {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (isV4Authentication) {
                            result.cryptoObject?.cipher?.let {
                                viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithBiometrics(it, loginCredentials))
                            }
                        } else {
                            // Use v3 authentication system
                            try {
                                appKeystore.getBiometricsDecryptionCipher(
                                    encryptedData,
                                    loginCredentials.keystore
                                ).also {
                                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithBiometricsV3(it, loginCredentials))
                                }

                            } catch (e: Exception) {
                                errorDialog(e)
                            }
                        }
                    }
                })
            try {

                // v4 uses a default keystore and a crypto object
                if(isV4Authentication){
                    biometricPrompt?.authenticate(
                        promptInfo.build(),
                        BiometricPrompt.CryptoObject(
                            appKeystore.getBiometricsDecryptionCipher(
                                encryptedData = encryptedData
                            )
                        )
                    )
                }else{
                    // v3 required only for user to be Authenticated
                     biometricPrompt?.authenticate(promptInfo.build())
                }

            } catch (e: KeyPermanentlyInvalidatedException) {
                errorSnackbar(e)
                // Remove invalidated login credentials
                viewModel.postEvent(LoginViewModel.LocalEvents.DeleteLoginCredentials(loginCredentials))
            } catch (e: UnrecoverableKeyException) {
                errorSnackbar(e)
                // Remove invalidated login credentials
                viewModel.postEvent(LoginViewModel.LocalEvents.DeleteLoginCredentials(loginCredentials))
            } catch (e: Exception) {
                errorDialog(e)
            }
        }
    }

    private fun launchUserPresencePromptForLightningShortcut() {
        biometricPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_authenticate))
            .setConfirmationRequired(true)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }

        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : AuthenticationCallback(this) {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    if(errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL){
                        // User hasn't enabled any device credential,
                        viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(true))
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(true))
                }
            })

        try {
            // Ask for user presence
            biometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            errorDialog(e) {
                // If an unsupported method is initiated, it's better to show the words rather than
                // block the user from retrieving his words
                viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(true))
            }
        }
    }
}


package com.blockstream.green.ui.wallet

import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Credentials
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.LoginFragmentBinding
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.bottomsheets.Bip39PassphraseBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.ui.settings.AppSettingsDialogFragment
import com.blockstream.green.utils.*
import com.blockstream.green.views.GreenAlertView
import com.blockstream.green.views.GreenPinViewListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.security.UnrecoverableKeyException
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : WalletFragment<LoginFragmentBinding>(
    layout = R.layout.login_fragment,
    menuRes = R.menu.login
) {
    val args: LoginFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }
    val device by lazy { deviceManager.getDevice(args.deviceId) }

    private var menuHelp: MenuItem? = null
    private var menuBip39Passphrase: MenuItem? = null
    private var menuRename: MenuItem? = null
    private var menuDelete: MenuItem? = null
    private var menuShowRecoveryPhrase: MenuItem? = null

    @Inject
    lateinit var viewModelFactory: LoginViewModel.AssistedFactory
    val viewModel: LoginViewModel by viewModels {
        LoginViewModel.provideFactory(viewModelFactory, args.wallet, device)
    }

    @Inject
    lateinit var appKeystore: AppKeystore

    @Inject
    lateinit var deviceManager: DeviceManager

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var greenWallet: GreenWallet

    var biometricPrompt : BiometricPrompt? = null

    override val screenName = "Login"

    override fun getBannerAlertView(): GreenAlertView = binding.banner

    override fun isLoggedInRequired(): Boolean = false

    override fun getWalletViewModel() = viewModel

    override val title: String
        get() = viewModel.wallet.name

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                if(it.data is Wallet) {
                    logger.info { "Login successful" }
                    navigate(LoginFragmentDirections.actionGlobalOverviewFragment(wallet = it.data))
                }else if (it.data is Credentials){
                    logger.info { "Emergency Recovery Phrase" }
                    navigate(LoginFragmentDirections.actionGlobalRecoveryPhraseFragment(wallet = null, credentials = it.data))
                }
            }

            onEvent.getContentIfNotHandledForType<LoginViewModel.LoginEvent.LaunchBiometrics>()?.let {
                if(args.autoLogin) {
                    launchBiometricPrompt(it.loginCredentials)
                }
            }

            onEvent.getContentIfNotHandledForType<LoginViewModel.LoginEvent.LoginDevice>()?.let {
                viewModel.loginWithDevice()
            }

            onEvent.getContentIfNotHandledForType<LoginViewModel.LoginEvent.AskBip39Passphrase>()?.let {
                Bip39PassphraseBottomSheetDialogFragment.show(childFragmentManager)
            }
        }

        viewModel.getWalletLiveData().observe(viewLifecycleOwner){
            updateToolbar()
        }

        viewModel.pinCredentials.observe(viewLifecycleOwner) {
            it?.also {

                if(it.counter > 0){
                    val errorMessage = if(it.counter == 2) {
                        getString(R.string.id_last_attempt_if_failed_you_will)
                    }else {
                        getString(R.string.id_invalid_pin_you_have_1d, 3 - it.counter)
                    }

                    binding.pinView.setError(errorMessage)
                }
            }

            updateMenu()
        }

        viewModel.passwordCredentials.observe(viewLifecycleOwner) {
            it?.also {

                if(it.counter > 0){
                    val errorMessage = if(it.counter == 2) {
                        getString(R.string.id_last_attempt_if_failed_you_will)
                    }else {
                        getString(R.string.id_invalid_pin_you_have_1d, 3 - it.counter)
                    }

                    binding.passwordLayout.error = errorMessage
                }
            }

            updateMenu()
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { _ ->
                binding.pinView.reset(true)
            }
        }

        viewModel.onErrorMessage.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { error ->
                if(device != null){
                    errorDialog(error) {
                        popBackStack()
                    }
                }else{
                    errorSnackbar(error)
                }
            }
        }

        binding.buttonAppSettings.setOnClickListener {
            AppSettingsDialogFragment.show(childFragmentManager)
        }

        binding.buttonLoginWithBiometrics.setOnClickListener {
            viewModel.biometricsCredentials.value?.let {
                launchBiometricPrompt(it)
            }
        }

        binding.pinView.listener = object : GreenPinViewListener {
            override fun onPin(pin: String) {
                viewModel.pinCredentials.value?.let { loginCredentials ->
                    viewModel.loginWithPin(pin, loginCredentials)
                }
            }

            override fun onPinChange(pinLength: Int, intermediatePin: String?) {}

            override fun onPinNotVerified() {}

            override fun onChangeMode(isVerify: Boolean) {}
        }

        binding.buttonRestoreWallet.setOnClickListener {
            navigate(
                LoginFragmentDirections.actionLoginFragmentToChooseRecoveryPhraseFragment(
                    OnboardingOptions.fromWallet(
                        viewModel.wallet, session.networkFromWallet(
                            viewModel.wallet
                        )
                    ), restoreWallet = wallet
                )
            )
        }

        binding.buttonWatchOnlyLogin.setOnClickListener {
            val watchOnlyCredentials = viewModel.keystoreCredentials.value

            if(viewModel.initialAction.value == false && watchOnlyCredentials != null) {
                viewModel.loginWatchOnlyWithKeyStore(watchOnlyCredentials)
            }else{
                viewModel.watchOnlyLogin()
            }
        }

        binding.buttonLoginWithPassword.setOnClickListener {
            viewModel.passwordCredentials.value?.let { loginCredentials ->
                viewModel.loginWithPin(viewModel.password.value ?: "", loginCredentials)
            }
            hideKeyboard()
        }

        binding.buttonLoginWithDevice.setOnClickListener {
            viewModel.loginWithDevice()
        }

        binding.passphraseButton.setOnClickListener {
            Bip39PassphraseBottomSheetDialogFragment.show(childFragmentManager)
        }

        binding.buttonEmergencyRecoveryPhrase.setOnClickListener {
            viewModel.isEmergencyRecoveryPhrase.value = false
        }
    }

    private fun updateMenu(){
        menuHelp?.isVisible = !wallet.isHardware && !viewModel.wallet.isWatchOnly && viewModel.pinCredentials.isReadyAndNull && viewModel.passwordCredentials.isReadyAndNull
        menuBip39Passphrase?.isVisible = !wallet.isHardware && !wallet.isWatchOnly
        menuRename?.isVisible = !wallet.isHardware
        menuDelete?.isVisible = !wallet.isHardware
        menuShowRecoveryPhrase?.isVisible = !wallet.isHardware && !wallet.isWatchOnly
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menuHelp = menu.findItem(R.id.help)
        menuBip39Passphrase = menu.findItem(R.id.bip39_passphrase)
        menuRename = menu.findItem(R.id.rename)
        menuDelete = menu.findItem(R.id.delete)
        menuShowRecoveryPhrase = menu.findItem(R.id.show_recovery_phrase)
        updateMenu()
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.bip39_passphrase -> {
                Bip39PassphraseBottomSheetDialogFragment.show(childFragmentManager)
            }
            R.id.delete -> {
                DeleteWalletBottomSheetDialogFragment.show(wallet, childFragmentManager)
            }
            R.id.rename -> {
                RenameWalletBottomSheetDialogFragment.show(wallet, childFragmentManager)
            }
            R.id.show_recovery_phrase -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.id_emergency_recovery_phrase_restore)
                    .setMessage(R.string.id_for_any_reason_you_cant)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.bip39Passphrase.value = ""
                        viewModel.isEmergencyRecoveryPhrase.value = true
                    }
                    .show()
            }
            R.id.help -> {
                openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_MNEMONIC_BACKUP)
            }
        }
        return super.onOptionsItemSelected(item)
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

        loginCredentials.encryptedData?.let { encryptedData ->
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
                                viewModel.loginWithBiometrics(it, loginCredentials)
                            }
                        } else {
                            // Use v3 authentication system
                            try {
                                appKeystore.getBiometricsDecryptionCipher(
                                    encryptedData,
                                    loginCredentials.keystore
                                ).also {
                                    viewModel.loginWithBiometricsV3(it, loginCredentials)
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
                viewModel.deleteLoginCredentials(loginCredentials)
            } catch (e: UnrecoverableKeyException) {
                errorSnackbar(e)
                // Remove invalidated login credentials
                viewModel.deleteLoginCredentials(loginCredentials)
            } catch (e: Exception) {
                errorDialog(e)
            }
        }
    }
}


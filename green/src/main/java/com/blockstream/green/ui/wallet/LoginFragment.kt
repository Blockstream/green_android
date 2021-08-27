package com.blockstream.green.ui.wallet

import android.content.Intent
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
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.LoginFragmentBinding
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.dialogs.showTorSinglesigWarningIfNeeded
import com.blockstream.green.utils.*
import com.blockstream.green.views.GreenPinViewListener
import com.greenaddress.Bridge
import com.greenaddress.greenbits.ui.TabbedMainActivity
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import dagger.hilt.android.AndroidEntryPoint
import java.security.UnrecoverableKeyException
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : WalletFragment<LoginFragmentBinding>(
    layout = R.layout.login_fragment,
    menuRes = R.menu.login
) {
    val args: LoginFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }
    val device by lazy { if(args.deviceId != 0 ) deviceManager.getDevice(args.deviceId) else null }

    private var menuHelp: MenuItem? = null
    private var menuRename: MenuItem? = null
    private var menuDelete: MenuItem? = null

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


    override fun isLoggedInRequired(): Boolean = false

    override fun getWalletViewModel() = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        viewModel.actionLogin.observe(viewLifecycleOwner) {
            if (it) {
                if(Bridge.useGreenModule) {
                    navigate(LoginFragmentDirections.actionGlobalOverviewFragment(viewModel.wallet))
                }else{
                    val intent = Intent(requireContext(), TabbedMainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }

        viewModel.getWalletLiveData().observe(viewLifecycleOwner) {
            setToolbar(viewModel.wallet)
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

        viewModel.biometricsCredentials.observe(viewLifecycleOwner) {
            it?.let {
                /**
                 * We can get multiple events, so launch prompt only the first time
                 */
                if(viewModel.initialAction.value == false){
                    viewModel.initialAction.value = true
                    launchBiometricPrompt(it)
                }
            }
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

        binding.buttonConnectionSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalConnectionSettingsDialogFragment())
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
             device?.let { it1 ->
                 viewModel.loginWithDevice(it1)
             }
        }

        device?.let { device ->
            if(viewModel.initialAction.value == false){
                viewModel.initialAction.value = true
                viewModel.loginWithDevice(device)
            }
        }

        // Show Singlesig Tor warning
        settingsManager.getApplicationSettingsLiveData().distinctUntilChanged().observe(viewLifecycleOwner) {
            it?.let { applicationSettings ->
                if(applicationSettings.tor && !session.networkFromWallet(wallet).supportTorConnection){
                    showTorSinglesigWarningIfNeeded(settingsManager)
                }
            }
        }
    }

    private fun updateMenu(){
        menuHelp?.isVisible = !wallet.isHardware && !viewModel.wallet.isWatchOnly && (viewModel.pinCredentials.value == null && viewModel.passwordCredentials.value == null)
        menuRename?.isVisible = !wallet.isHardware
        menuDelete?.isVisible = !wallet.isHardware
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menuHelp = menu.findItem(R.id.help)
        menuRename = menu.findItem(R.id.rename)
        menuDelete = menu.findItem(R.id.delete)
        updateMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                DeleteWalletBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.toString())
                }
            }

            R.id.rename -> {
                RenameWalletBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.toString())
                }
            }

            R.id.help -> {
                openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_MNEMONIC_BACKUP)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchBiometricPrompt(loginCredentials: LoginCredentials) {
        biometricPrompt?.cancelAuthentication()

        val isV4Authentication = loginCredentials.keystore.isNullOrBlank()

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
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
                    // SDK 30
                    promptInfo.setAllowedAuthenticators(BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                } else {
                    promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
            }


            biometricPrompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        handleBiometricsError(errorCode, errString)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)

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

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
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


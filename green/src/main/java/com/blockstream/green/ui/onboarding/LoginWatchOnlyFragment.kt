package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.LoginWatchOnlyFragmentBinding
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.extensions.authenticateWithBiometrics
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.login.LoginFragmentDirections
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.getClipboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import java.security.InvalidAlgorithmParameterException
import javax.crypto.Cipher
import javax.inject.Inject

@AndroidEntryPoint
class LoginWatchOnlyFragment :
    AbstractOnboardingFragment<LoginWatchOnlyFragmentBinding>(
        R.layout.login_watch_only_fragment,
        menuRes = 0
    ) {
    override val screenName = "OnBoardWatchOnlyCredentials"

    val args: LoginWatchOnlyFragmentArgs by navArgs()

    var openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.also {
            viewModel.importFile(requireContext().contentResolver, it)
        }
    }

    private var biometricsCipher: CompletableDeferred<Cipher>? = null

    @Inject
    lateinit var appKeystore: AppKeystore

    @Inject
    lateinit var assistedFactory: LoginWatchOnlyViewModel.AssistedFactory

    val viewModel: LoginWatchOnlyViewModel by viewModels{
        LoginWatchOnlyViewModel.provideFactory(
            assistedFactory = assistedFactory, onboardingOptions = args.onboardingOptions
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.isSinglesig = args.onboardingOptions.isSinglesig

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.appendWatchOnlyDescriptor(it)
            }
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, fragmentManager = childFragmentManager)
        }

        binding.buttonLogin.setOnClickListener {
            viewModel.createNewWatchOnlyWallet(biometricsCipherProvider = lifecycleScope.async(start = CoroutineStart.LAZY) {

                biometricsCipher = CompletableDeferred<Cipher>()

                getBiometricsCipher()

                biometricsCipher!!.await()
            })
        }

        binding.descriptorType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked) {
                viewModel.isOutputDescriptors.value = checkedId == R.id.descriptor
            }
        }

        binding.buttonClear.setOnClickListener {
            viewModel.watchOnlyDescriptor.value = ""
        }

        binding.buttonPaste.setOnClickListener {
            getClipboard(requireContext())?.let { viewModel.appendWatchOnlyDescriptor(it) }
        }

        binding.buttonImport.setOnClickListener {
            openDocument.launch(
                arrayOf(
                    "application/json"
                )
            )
        }

        viewModel.isOutputDescriptors.distinctUntilChanged().observe(viewLifecycleOwner){
            binding.descriptorType.check(if(it) R.id.descriptor else R.id.xpub)
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                hideKeyboard()
                navigate(LoginFragmentDirections.actionGlobalWalletOverviewFragment(navigate.data as Wallet))
            }
        }
    }

    private fun getBiometricsCipher(onlyDeviceCredentials: Boolean = false) {

        if (appKeystore.isBiometricsAuthenticationRequired()) {
            authenticateWithBiometrics(object : AuthenticationCallback(fragment = this) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    getBiometricsCipher(onlyDeviceCredentials = false)
                }
            }, onlyDeviceCredentials = onlyDeviceCredentials)
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_login_with_biometrics))
            .setDescription(getString(R.string.id_green_uses_biometric))
            .setNegativeButtonText(getString(R.string.id_cancel))
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : AuthenticationCallback(fragment = this) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.also {
                        biometricsCipher?.complete(it)
                    } ?: kotlin.run {
                        biometricsCipher?.completeExceptionally(Exception("No Cipher Provided"))
                    }
                }

                override fun onAuthenticationFailed() { }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
                        // This is errorCode OK, no need to handle it
                        biometricsCipher?.completeExceptionally(Exception("id_action_canceled"))
                    } else {
                        biometricsCipher?.completeExceptionally(Exception(context.getString(R.string.id_authentication_error_s, "$errorCode $errString")))
                    }
                }
            })

        try {
            biometricPrompt.authenticate(
                promptInfo.build(),
                BiometricPrompt.CryptoObject(appKeystore.getBiometricsEncryptionCipher())
            )
        } catch (e: InvalidAlgorithmParameterException) {
            // At least one biometric must be enrolled
            errorDialog(getString(R.string.id_please_activate_at_least_one))
        } catch (e: Exception) {
            errorDialog(e)
        }
    }
}
package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.WatchOnlyCredentialsViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.WatchOnlyCredentialsFragmentBinding
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.extensions.authenticateWithBiometrics
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.getClipboard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okio.source
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.security.InvalidAlgorithmParameterException
import javax.crypto.Cipher

class WatchOnlyCredentialsFragment : AppFragment<WatchOnlyCredentialsFragmentBinding>(
    R.layout.watch_only_credentials_fragment,
    menuRes = 0
) {
    val args: WatchOnlyCredentialsFragmentArgs by navArgs()

    var openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.also {
            requireContext().contentResolver.openInputStream(uri)?.source()?.also {
                viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.ImportFile(it))
            }
        }
    }

    private var biometricsCipher: CompletableDeferred<Cipher>? = null

    private val appKeystore: AppKeystore by inject()

    val viewModel: WatchOnlyCredentialsViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel() = null

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        ((sideEffect as? SideEffects.NavigateTo)?.destination as? NavigateDestinations.WalletOverview)?.also {
            navigate(NavGraphDirections.actionGlobalWalletOverviewFragment(it.greenWallet))
        }

        if(sideEffect is WatchOnlyCredentialsViewModel.LocalSideEffects.RequestCipher){
            getBiometricsCipher()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.AppendWatchOnlyDescriptor(it.result))
            }
        }

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, decodeContinuous = true, fragmentManager = childFragmentManager)
        }

        binding.buttonLogin.setOnClickListener {
            hideKeyboard()
            viewModel.postEvent(Events.Continue)
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
            getClipboard(requireContext())?.let {
                viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.AppendWatchOnlyDescriptor(it))
            }
        }

        binding.buttonImport.setOnClickListener {
            openDocument.launch(
                arrayOf(
                    "application/json"
                )
            )
        }

        viewModel.isOutputDescriptors.onEach {
            binding.descriptorType.check(if(it) R.id.descriptor else R.id.xpub)
        }.launchIn(lifecycleScope)
    }

    private fun getBiometricsCipher(onlyDeviceCredentials: Boolean = false) {

        if (appKeystore.isBiometricsAuthenticationRequired()) {
            authenticateWithBiometrics(object : AuthenticationCallback(fragment = this) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    getBiometricsCipher(onlyDeviceCredentials = true)
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
                        viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.ProvideCipher(platformCipher = it))
                    } ?: kotlin.run {
                        Exception("No Cipher Provided").also {
                            biometricsCipher?.completeExceptionally(it)
                            viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.ProvideCipher(exception = it))
                        }
                    }
                }

                override fun onAuthenticationFailed() { }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
                        // This is errorCode OK, no need to handle it
                        Exception("id_action_canceled").also {
                            biometricsCipher?.completeExceptionally(it)
                            viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.ProvideCipher(exception = it))
                        }

                    } else {
                        Exception(context.getString(R.string.id_authentication_error_s, "$errorCode $errString")).also {
                            biometricsCipher?.completeExceptionally(it)
                            viewModel.postEvent(WatchOnlyCredentialsViewModel.LocalEvents.ProvideCipher(exception = it))
                        }
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
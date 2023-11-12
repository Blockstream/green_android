package com.blockstream.green.ui.recovery

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.arkivanov.essenty.statekeeper.stateKeeper
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryIntroFragmentBinding
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryIntroFragment : AppFragment<RecoveryIntroFragmentBinding>(
    layout = R.layout.recovery_intro_fragment,
    menuRes = 0
) {
    private var biometricPrompt: BiometricPrompt? = null

    private val args: RecoveryIntroFragmentArgs by navArgs()

    private val viewModel : RecoveryIntroViewModel by viewModel {
        parametersOf(args.setupArgs, stateKeeper())
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonNext.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }

        binding.toggleRecoverySize.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked){
                viewModel.mnemonicSize.value = if(checkedId == R.id.button12) 12 else 24
            }
        }

        binding.toggleRecoverySize.check(if (viewModel.mnemonicSize.value == 12) R.id.button12 else R.id.button24)
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.NavigateTo){
            (sideEffect.destination as? NavigateDestinations.RecoveryWords)?.also {
                navigate(
                    RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryWordsFragment(
                        args = it.args
                    )
                )
            }

            (sideEffect.destination as? NavigateDestinations.RecoveryPhrase)?.also {
                navigate(
                    RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryPhraseFragment(
                        wallet = it.args.greenWallet,
                        isLightning = it.args.isLightning
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, true)
                    }
                )
            }
        } else if (sideEffect is RecoveryIntroViewModel.LocalSideEffects.LaunchUserPresence) {
            launchUserPresencePrompt()
        }
    }

    private fun launchUserPresencePrompt() {
        biometricPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_authenticate_to_view_the))
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
                        viewModel.postEvent(RecoveryIntroViewModel.LocalEvents.Authenticated(false))
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.postEvent(RecoveryIntroViewModel.LocalEvents.Authenticated(true))
                }
            })

        try {
            // Ask for user presence
            biometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            errorDialog(e) {
                // If an unsupported method is initiated, it's better to show the words rather than
                // block the user from retrieving his words
                viewModel.postEvent(RecoveryIntroViewModel.LocalEvents.Authenticated(false))
            }
        }
    }
}

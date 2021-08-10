package com.blockstream.green.ui.recovery

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryIntroFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.utils.handleBiometricsError
import com.greenaddress.Bridge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecoveryIntroFragment : WalletFragment<RecoveryIntroFragmentBinding>(
    layout = R.layout.recovery_intro_fragment,
    menuRes = 0
) {
    private var biometricPrompt: BiometricPrompt? = null

    private val args: RecoveryIntroFragmentArgs by navArgs()

    override val wallet by lazy { args.wallet!! }

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionRequired(): Boolean {
        return args.wallet != null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isFinishingGuard) return

        binding.buttonNext.setOnClickListener {

            if(args.wallet != null) {

                // If recovery is confirmed, ask for user presence
                if (wallet.isRecoveryPhraseConfirmed) {
                    launchUserPresencePrompt()
                } else {
                    navigateToWords()
                }

            }else{
                navigateToWords()
            }
        }
    }

    private fun navigateToWords(){
        // prototype or onboarding
        if (Bridge.useGreenModule || args.wallet == null){
            navigate(
                RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryWordsFragment(
                    wallet = args.wallet,
                    onboardingOptions = args.onboardingOptions,
                    mnemonic = args.mnemonic
                )
            )
        }else{
            navigate(RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryPhraseFragment(wallet = args.wallet), navOptionsBuilder = NavOptions.Builder().also {
                it.setPopUpTo(R.id.recoveryIntroFragment, true)
            })
        }
    }

    private fun launchUserPresencePrompt() {
        biometricPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.id_authenticate_to_view_the))
            .setConfirmationRequired(true)

        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
            // SDK 30
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
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

                    if(errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL){
                        // User hasn't enabled any device credential,
                        navigateToWords()
                    }else{
                        handleBiometricsError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToWords()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        try {
            // Ask for user presence
            biometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            errorDialog(e) {
                // If an unsupported method is initiated, it's better to show the words rather than
                // block the user to retrieve his words
                navigateToWords()
            }
        }
    }

    override fun getWalletViewModel(): AbstractWalletViewModel? = null
}

package com.blockstream.green.ui.recovery

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryIntroFragmentBinding
import com.blockstream.green.extensions.AuthenticationCallback
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.extensions.errorDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryIntroFragment : AbstractWalletFragment<RecoveryIntroFragmentBinding>(
    layout = R.layout.recovery_intro_fragment,
    menuRes = 0
) {
    private var biometricPrompt: BiometricPrompt? = null

    private val args: RecoveryIntroFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }
    private val networkOrNull by lazy { args.network }

    override val screenName = "RecoveryIntro"

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, args.wallet!!)
    }

    @Inject
    lateinit var introViewModelFactory: RecoveryIntroViewModel.AssistedFactory
    val introViewModel: RecoveryIntroViewModel by viewModels {
        RecoveryIntroViewModel.provideFactory(introViewModelFactory, this, arguments, !args.isAuthenticateUser)
    }

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionAndWalletRequired(): Boolean {
        return args.wallet != null
    }

    override fun isLoggedInRequired(): Boolean = isSessionAndWalletRequired()

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.showRecoveryLength = !args.isAuthenticateUser

        binding.buttonNext.setOnClickListener {

            // Onboarding
            if(args.wallet == null){
                navigateToWords()
            }else{
                if(args.isAuthenticateUser) {
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

        binding.toggleRecoverySize.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked){
                introViewModel.recoverySize.value = checkedId
            }
        }

        binding.toggleRecoverySize.check(introViewModel.recoverySize.value ?: R.id.button12)
    }

    private fun navigateToWords() {
        // Onboarding
        if (args.wallet == null || !args.isAuthenticateUser) {
            navigate(
                RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryWordsFragment(
                    wallet = args.wallet,
                    assetId = args.assetId,
                    onboardingOptions = args.onboardingOptions,
                    mnemonic = introViewModel.mnemonic,
                    network = args.network
                )
            )
        } else {
            navigate(
                RecoveryIntroFragmentDirections.actionRecoveryIntroFragmentToRecoveryPhraseFragment(
                    wallet = args.wallet,
                ), navOptionsBuilder = NavOptions.Builder().also {
                    it.setPopUpTo(R.id.recoveryIntroFragment, true)
                })
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
                        navigateToWords()
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    navigateToWords()
                }
            })

        try {
            // Ask for user presence
            biometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            errorDialog(e) {
                // If an unsupported method is initiated, it's better to show the words rather than
                // block the user from retrieving his words
                navigateToWords()
            }
        }
    }

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getAppViewModel() : AppViewModel? = if(args.wallet == null) null else getWalletViewModel()

    override fun getWalletViewModel(): AbstractWalletViewModel = if(args.wallet != null) viewModel else throw RuntimeException("Can't be happening")
}

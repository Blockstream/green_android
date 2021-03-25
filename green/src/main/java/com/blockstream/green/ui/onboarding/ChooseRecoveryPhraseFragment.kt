package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseRecoveryPhraseFragmentBinding

class ChooseRecoveryPhraseFragment :
    AbstractOnboardingFragment<ChooseRecoveryPhraseFragmentBinding>(R.layout.choose_recovery_phrase_fragment, menuRes = 0) {

    val args: ChooseRecoveryPhraseFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        binding.cardQRCode.setOnClickListener {
            navigate(ChooseRecoveryPhraseFragmentDirections.actionChooseRecoveryPhraseFragmentToRecoveryScanQRFragment(options!!, restoreWallet = args.restoreWallet))
        }

        binding.cardRecoveryPhrase.setOnClickListener {
            navigate(ChooseRecoveryPhraseFragmentDirections.actionChooseRecoveryPhraseFragmentToEnterRecoveryPhraseFragment(options!!, restoreWallet = args.restoreWallet))
        }
    }

}
package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseRecoveryPhraseFragmentBinding
import com.blockstream.green.ui.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.overview.OverviewFragment
import com.blockstream.green.utils.clearNavigationResult
import com.blockstream.green.utils.getNavigationResult
import com.blockstream.green.utils.snackbar

class ChooseRecoveryPhraseFragment :
    AbstractOnboardingFragment<ChooseRecoveryPhraseFragmentBinding>(R.layout.choose_recovery_phrase_fragment, menuRes = 0) {

    val args: ChooseRecoveryPhraseFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let { result ->
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                navigate(ChooseRecoveryPhraseFragmentDirections.actionChooseRecoveryPhraseFragmentToEnterRecoveryPhraseFragment(
                    args.onboardingOptions, result, restoreWallet = args.restoreWallet
                ))
            }
        }

        options = args.onboardingOptions

        binding.cardQRCode.setOnClickListener {
            CameraBottomSheetDialogFragment.open(this)
        }

        binding.cardRecoveryPhrase.setOnClickListener {
            navigate(ChooseRecoveryPhraseFragmentDirections.actionChooseRecoveryPhraseFragmentToEnterRecoveryPhraseFragment(options!!, restoreWallet = args.restoreWallet))
        }
    }

}
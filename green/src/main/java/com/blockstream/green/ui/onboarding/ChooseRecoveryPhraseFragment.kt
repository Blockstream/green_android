package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseRecoveryPhraseFragmentBinding
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.utils.clearNavigationResult
import com.blockstream.green.utils.getNavigationResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChooseRecoveryPhraseFragment :
    AbstractOnboardingFragment<ChooseRecoveryPhraseFragmentBinding>(R.layout.choose_recovery_phrase_fragment, menuRes = 0) {

    val args: ChooseRecoveryPhraseFragmentArgs by navArgs()

    override val screenName = "OnBoardChooseRecovery"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                navigate(ChooseRecoveryPhraseFragmentDirections.actionChooseRecoveryPhraseFragmentToEnterRecoveryPhraseFragment(
                    args.onboardingOptions, result, wallet = args.restoreWallet
                ))
            }
        }

        options = args.onboardingOptions

        binding.cardQRCode.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(childFragmentManager)
        }

        binding.cardRecoveryPhrase.setOnClickListener {
            navigate(ChooseRecoveryPhraseFragmentDirections.actionChooseRecoveryPhraseFragmentToEnterRecoveryPhraseFragment(options, wallet = args.restoreWallet))
        }
    }

}
package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseWatchOnlyFragmentBinding
import com.blockstream.green.ui.bottomsheets.ComingSoonBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChooseWatchOnlyFragment :
    AbstractOnboardingFragment<ChooseWatchOnlyFragmentBinding>(
        R.layout.choose_watch_only_fragment,
        menuRes = 0
    ) {

    override val screenName = "OnBoardWatchOnlyChooseSecurity"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonMultisig.setOnClickListener {
            navigate(
                ChooseWatchOnlyFragmentDirections.actionChooseWatchOnlyFragmentToChooseNetworkFragment(
                    OnboardingOptions(isRestoreFlow = true, isWatchOnly = true, isSinglesig = false)
                )
            )
        }

        binding.buttonSinglesig.setOnClickListener {
            ComingSoonBottomSheetDialogFragment().also {
                it.show(childFragmentManager, it.toString())
            }
//            navigate(
//                ChooseWatchOnlyFragmentDirections.actionChooseWatchOnlyFragmentToLoginWatchOnlyFragment(isMultisig = false)
//            )
        }
    }
}
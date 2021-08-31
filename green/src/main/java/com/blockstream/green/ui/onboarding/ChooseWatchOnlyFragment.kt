package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseWatchOnlyFragmentBinding
import com.blockstream.green.ui.ComingSoonBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChooseWatchOnlyFragment :
    AbstractOnboardingFragment<ChooseWatchOnlyFragmentBinding>(
        R.layout.choose_watch_only_fragment,
        menuRes = 0
    ) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGreenWatchOnly.setOnClickListener {
            navigate(
                ChooseWatchOnlyFragmentDirections.actionChooseWatchOnlyFragmentToLoginWatchOnlyFragment()
            )
        }

        binding.buttonWatchOnly.setOnClickListener {
            ComingSoonBottomSheetDialogFragment().also {
                it.show(childFragmentManager, it.toString())
            }
        }
    }
}
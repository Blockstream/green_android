package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.OnboardingCompleteFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnBoardingCompleteFragment :
    AbstractOnboardingFragment<OnboardingCompleteFragmentBinding>(
        R.layout.onboarding_complete_fragment,
        menuRes = 0
    ) {

    private val args: OnBoardingCompleteFragmentArgs by navArgs()

    private val onBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        binding.titleString = getString(R.string.id_success)

        options?.apply {
            if(isRestoreFlow){
                binding.primaryButtonString = getString(R.string.id_done)
            }else{
                binding.primaryButtonString = getString(R.string.id_explore_your_wallet)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)

        binding.buttonPrimary.setOnClickListener {
            openOverview()
        }
    }
}

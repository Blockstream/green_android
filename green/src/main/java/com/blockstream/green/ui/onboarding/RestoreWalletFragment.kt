package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.RestoreWalletFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RestoreWalletFragment :
    AbstractOnboardingFragment<RestoreWalletFragmentBinding>(R.layout.restore_wallet_fragment, menuRes = 0) {

    val args: RestoreWalletFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        binding.cardRestoreGreen.setOnClickListener {
            options?.apply {
             navigate(copy(isBIP39 = false))
            }
        }

        // Disabled for now
        binding.cardMigrate.disable()
//        binding.cardMigrate.setOnClickListener {
//            options?.apply {
//                navigate(copy(isBIP39 = true))
//            }
//        }
    }

    fun navigate(options: OnboardingOptions){
        if(options.isBIP39){
            navigate(RestoreWalletFragmentDirections.actionRestoreWalletFragmentToChooseRecoveryPhraseFragment(options))
        }else{
            navigate(RestoreWalletFragmentDirections.actionRestoreWalletFragmentToChooseNetworkFragment(options))
        }
    }
}
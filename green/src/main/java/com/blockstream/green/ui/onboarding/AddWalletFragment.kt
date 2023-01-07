package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.AddWalletFragmentBinding
import com.blockstream.green.ui.bottomsheets.EnvironmentBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.EnvironmentListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddWalletFragment :
    AbstractOnboardingFragment<AddWalletFragmentBinding>(R.layout.add_wallet_fragment, menuRes = 0), EnvironmentListener {

    override val screenName = "AddWallet"
    private var isRestore: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonNewWallet.setOnClickListener {
            if (settingsManager.getApplicationSettings().testnet){
                askForNetworkEnviroment(false)
            }else{
                navigate(
                    AddWalletFragmentDirections.actionAddWalletFragmentToRecoveryIntroFragment(onboardingOptions = OnboardingOptions(isRestoreFlow = false))
                )
            }

            countly.newWallet()
        }

        binding.buttonRestoreWallet.setOnClickListener {
            if (settingsManager.getApplicationSettings().testnet){
                askForNetworkEnviroment(true)
            }else{
                navigate(
                    AddWalletFragmentDirections.actionAddWalletFragmentToEnterRecoveryPhraseFragment(OnboardingOptions(isRestoreFlow = true))
                )
            }

            countly.restoreWallet()
        }

        binding.buttonWatchOnly.setOnClickListener {
            navigate(AddWalletFragmentDirections.actionAddWalletFragmentToChooseWatchOnlyFragment())
            countly.watchOnlyWallet()
        }
    }

    private fun askForNetworkEnviroment(isRestore: Boolean){
        this.isRestore = isRestore
        EnvironmentBottomSheetDialogFragment.show(childFragmentManager)
    }

    override fun onEnvironmentSelected(isTestnet: Boolean?) {
        if(isTestnet != null){
            val onboardingOptions = OnboardingOptions(isRestoreFlow = isRestore, isTestnet = isTestnet)

            navigate(
                if(onboardingOptions.isRestoreFlow){
                    AddWalletFragmentDirections.actionAddWalletFragmentToEnterRecoveryPhraseFragment(onboardingOptions = onboardingOptions)
                }else{
                    AddWalletFragmentDirections.actionAddWalletFragmentToRecoveryIntroFragment(onboardingOptions = onboardingOptions)
                }
            )
        }
    }
}
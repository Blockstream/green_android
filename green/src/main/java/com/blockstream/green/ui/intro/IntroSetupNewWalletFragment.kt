package com.blockstream.green.ui.intro

import com.blockstream.green.ui.onboarding.SetupNewWalletFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroSetupNewWalletFragment : SetupNewWalletFragment() {


//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.vm = viewModel
//
//        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
//            viewModel.termsChecked.value = isChecked
//        }
//
//        binding.buttonAddWallet.setOnClickListener {
//            askForAnalyticsConsentAndNavigate(SetupNewWalletFragmentDirections.actionSetupNewWalletFragmentToAddWalletFragment())
//            countly.addWallet()
//        }
//
//        binding.buttonHardware.setOnClickListener {
//            askForAnalyticsConsentAndNavigate(SetupNewWalletFragmentDirections.actionSetupNewWalletFragmentToUseHardwareDeviceFragment())
//            settingsManager.setDeviceTermsAccepted()
//            countly.hardwareWallet()
//        }
//
//        binding.buttonAppSettings.setOnClickListener {
//            AppSettingsDialogFragment.show(childFragmentManager)
//        }
//    }

}
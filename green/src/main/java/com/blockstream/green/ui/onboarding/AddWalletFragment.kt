package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.AddWalletFragmentBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.devices.DeviceInfoFragmentDirections
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddWalletFragment :
    AbstractOnboardingFragment<AddWalletFragmentBinding>(R.layout.add_wallet_fragment, menuRes = 0) {

    val args: AddWalletFragmentArgs by navArgs()

    @Inject
    lateinit var assistedFactory: AddWalletViewModel.AssistedFactory

    private val viewModel: AddWalletViewModel by viewModels {
        AddWalletViewModel.provideFactory(assistedFactory, args.deviceId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.termsChecked.value = isChecked
        }

        binding.buttonContinueHardware.setOnClickListener {
            settingsManager.setDeviceTermsAccepted()
            setNavigationResult(result = args.network)
            popBackStack()
        }

        binding.buttonNewWallet.setOnClickListener {
            val options = OnboardingOptions(isRestoreFlow = false)
            navigate(AddWalletFragmentDirections.actionAddWalletFragmentToChooseNetworkFragment(options))
        }

        binding.buttonRestoreWallet.setOnClickListener {
            val options = OnboardingOptions(isRestoreFlow = true)
            navigate(AddWalletFragmentDirections.actionAddWalletFragmentToChooseNetworkFragment(options))
        }

        binding.buttonWatchOnly.setOnClickListener {
             navigate(AddWalletFragmentDirections.actionAddWalletFragmentToChooseWatchOnlyFragment())
        }

        binding.termsLink.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.TERMS_OF_SERVICE)
        }
    }
}
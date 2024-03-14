package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.ChangePinFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.views.GreenPinViewListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChangePinFragment : AppFragment<ChangePinFragmentBinding>(R.layout.change_pin_fragment, menuRes = 0) {

    val args : WalletSettingsFragmentArgs by navArgs()

    val viewModel: WalletSettingsViewModel by viewModel {
        parametersOf(args.wallet, null, WalletSettingsSection.ChangePin)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.pinView.isVerifyMode = true
        binding.pinView.listener = object : GreenPinViewListener{
            override fun onPin(pin: String) {
                viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SetPin(pin = pin))
            }

            override fun onPinChange(pinLength: Int, intermediatePin: String?) {

            }

            override fun onPinNotVerified() {
                Toast.makeText(requireContext(), R.string.id_pins_do_not_match_please_try, Toast.LENGTH_SHORT).show()
            }

            override fun onChangeMode(isVerify: Boolean) {
                binding.title.setText(if(isVerify) R.string.id_verify_your_pin else R.string.id_change_pin)
            }
        }
    }
}
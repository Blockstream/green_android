package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.databinding.ChangePinFragmentBinding
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.views.GreenPinViewListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangePinFragment : AbstractWalletFragment<ChangePinFragmentBinding>(R.layout.change_pin_fragment, menuRes = 0) {

    val args : WalletSettingsFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "WalletSettingsChangePIN"

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by viewModels {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        binding.pinView.isVerifyMode = true
        binding.pinView.listener = object : GreenPinViewListener{
            override fun onPin(pin: String) {
                viewModel.changePin(pin)
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

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledForType<GdkEvent.Success>()?.let {
                snackbar(R.string.id_you_have_successfully_changed)
                findNavController().popBackStack()
            }
        }
    }
}
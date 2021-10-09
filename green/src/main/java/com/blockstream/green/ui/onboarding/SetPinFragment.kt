package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.SetPinFragmentBinding
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.utils.hideKeyboard
import com.blockstream.green.views.GreenPinViewListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SetPinFragment : AbstractOnboardingFragment<SetPinFragmentBinding>(R.layout.set_pin_fragment, menuRes = 0) {

    private val args: SetPinFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelFactory: SetPinViewModel.AssistedFactory
    val viewModel: SetPinViewModel by viewModels {
        SetPinViewModel.provideFactory(viewModelFactory, args.onboardingOptions, args.restoreWallet)
    }

    private var pin: String = ""

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        options = args.onboardingOptions

        binding.pinView.isVerifyMode = true
        binding.pinView.listener = object : GreenPinViewListener{
            override fun onPin(pin: String) {
                this@SetPinFragment.pin = pin
                viewModel.isPinVerified.value = true
            }

            override fun onPinChange(pinLength: Int, intermediatePin: String?) {
                this@SetPinFragment.pin = ""
                viewModel.isPinVerified.value = false
            }

            override fun onPinNotVerified() {
                Toast.makeText(requireContext(), R.string.id_pins_do_not_match_please_try, Toast.LENGTH_SHORT).show()
            }

            override fun onChangeMode(isVerify: Boolean) {
                binding.title.setText(if(isVerify) R.string.id_verify_your_pin else R.string.id_set_a_pin)
            }
        }

        binding.buttonNext.setOnClickListener {
            options?.let {
                if(it.isRestoreFlow){
                    viewModel.restoreWithPin(it, pin)
                }else{
                    viewModel.createNewWallet(it, pin, args.mnemonic)
                }
            }
        }

        viewModel.onProgress.observe(viewLifecycleOwner){
            setToolbarVisibility(!it)
            onBackCallback.isEnabled = it
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                errorDialog(throwable)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                navigate(SetPinFragmentDirections.actionSetPinFragmentToOnBoardingCompleteFragment(options!!, navigate.data as Wallet))
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    override fun onResume() {
        super.onResume()
        hideKeyboard()
    }
}
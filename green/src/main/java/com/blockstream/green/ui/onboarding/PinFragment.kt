package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.onboarding.PinViewModel
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.PinFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.MainActivity
import com.blockstream.green.views.GreenPinViewListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PinFragment : AppFragment<PinFragmentBinding>(R.layout.pin_fragment, menuRes = 0) {
    private val args: PinFragmentArgs by navArgs()

    private val viewModel: PinViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel() = null

    private var pin: String = ""

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        ((sideEffect as? SideEffects.NavigateTo)?.destination as? NavigateDestinations.WalletOverview)?.also {
            navigate(NavGraphDirections.actionGlobalWalletOverviewFragment(it.greenWallet))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.isPinVerified = false
        binding.pinView.isVerifyMode = true
        binding.pinView.listener = object : GreenPinViewListener{
            override fun onPin(pin: String) {
                this@PinFragment.pin = pin
                binding.isPinVerified = true
            }

            override fun onPinChange(pinLength: Int, intermediatePin: String?) {
                this@PinFragment.pin = ""
                binding.isPinVerified = false
            }

            override fun onPinNotVerified() {
                Toast.makeText(requireContext(), R.string.id_pins_do_not_match_please_try, Toast.LENGTH_SHORT).show()
            }

            override fun onChangeMode(isVerify: Boolean) {
                binding.title.setText(if(isVerify) R.string.id_verify_your_pin else R.string.id_set_a_pin)
            }
        }

        binding.buttonNext.setOnClickListener {
            viewModel.postEvent(PinViewModel.LocalEvents.SetPin(pin))
        }

        viewModel.onProgress.onEach {
            setToolbarVisibility(!it)
        }.launchIn(lifecycleScope)

        viewModel.navigationLock.onEach {
            onBackCallback.isEnabled = it
            (requireActivity() as MainActivity).lockDrawer(it)
        }.launchIn(lifecycleScope)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }
}
package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.blockstream.green.R
import com.blockstream.green.databinding.TwofactorResetFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.errorDialog
import com.greenaddress.Bridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class TwoFactorResetAction {
    RESET, CANCEL, DISPUTE, UNDO_DISPUTE
}

@AndroidEntryPoint
class TwoFactorResetFragment :
    WalletFragment<TwofactorResetFragmentBinding>(R.layout.twofactor_reset_fragment, 0) {
    val args: TwoFactorResetFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    override val isAdjustResize: Boolean = true

    @Inject
    lateinit var viewModelFactory: WalletSettingsViewModel.AssistedFactory
    val viewModel: WalletSettingsViewModel by navGraphViewModels(R.id.settings_nav_graph) {
        WalletSettingsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.actionType = args.actionType

        when(args.actionType){
            TwoFactorResetAction.RESET -> {
                setToolbar(title = getString(R.string.id_request_twofactor_reset))
                binding.message = getString(R.string.id_resetting_your_twofactor_takes)
                binding.action = getString(R.string.id_request_twofactor_reset)
            }
            TwoFactorResetAction.CANCEL -> {
                setToolbar(title = getString(R.string.id_cancel_2fa_reset))
                
                // Cancel action
                viewModel.cancel2FA(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
            }
            TwoFactorResetAction.DISPUTE -> {
                setToolbar(title = getString(R.string.id_dispute_twofactor_reset))
                binding.message = getString(R.string.id_if_you_did_not_request_the)
                binding.action = getString(R.string.id_dispute_twofactor_reset)
            }
            TwoFactorResetAction.UNDO_DISPUTE -> {
                setToolbar(title = getString(R.string.id_undo_2fa_dispute))
                binding.message = getString(R.string.id_if_you_initiated_the_2fa_reset)
                binding.action = getString(R.string.id_undo_2fa_dispute)
            }
        }

        binding.buttonContinue.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            when(args.actionType){
                TwoFactorResetAction.RESET -> {
                    viewModel.reset2FA(
                        email = email,
                        isDispute = false,
                        twoFactorResolver = DialogTwoFactorResolver(requireContext())
                    )
                }
                TwoFactorResetAction.DISPUTE -> {
                    viewModel.reset2FA(
                        email = email,
                        isDispute = true,
                        twoFactorResolver = DialogTwoFactorResolver(requireContext())
                    )
                }
                TwoFactorResetAction.UNDO_DISPUTE -> {
                    viewModel.undoReset2FA(
                        email = email,
                        twoFactorResolver = DialogTwoFactorResolver(requireContext())
                    )
                }
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it) {
                    if(args.actionType == TwoFactorResetAction.CANCEL){
                        popBackStack()
                    }
                }
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                popBackStack()
            }
        }
    }

    private fun popBackStack(){
        if(Bridge.useGreenModule){
            findNavController().popBackStack()
        }else{
            requireActivity().finish()
        }
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel
}

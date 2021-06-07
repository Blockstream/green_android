package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryCheckFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.snackbar
import com.greenaddress.Bridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryCheckFragment : WalletFragment<RecoveryCheckFragmentBinding>(
    layout = R.layout.recovery_check_fragment,
    menuRes = 0
) {
    @Inject
    lateinit var viewModelFactory: RecoveryCheckViewModel.AssistedFactory

    private val viewModel: RecoveryCheckViewModel by viewModels {
        RecoveryCheckViewModel.provideFactory(
            viewModelFactory,
            args.wallet,
            (args.mnemonic ?: session.getMnemonicPassphrase()).split(" "),
            args.page,
            requireContext().isDevelopmentFlavor()
        )
    }

    private val args: RecoveryCheckFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet!! }

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionRequired(): Boolean {
        return args.wallet != null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.isDevelopmentFlavor = requireContext().isDevelopmentFlavor()

        viewModel.navigate.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledOrReturnNull()?.let { success ->
                if(success){
                    if(viewModel.isLastPage){

                        if(args.wallet != null) {

                            if (Bridge.useGreenModule) {
                                // Back to Overview
                                findNavController().popBackStack(R.id.recoveryIntroFragment, true)
                                setSecureScreen(false)
                            } else {
                                requireActivity().finish()
                            }

                        }else{
                            navigate(RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentToWalletNameFragment(
                                onboardingOptions = args.onboardingOptions!!,
                                mnemonic = args.mnemonic!!,
                                mnemonicPassword = "")
                            )
                        }
                    }else{
                        navigate(
                            RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentSelf(
                                wallet = args.wallet,
                                onboardingOptions = args.onboardingOptions,
                                mnemonic = args.mnemonic,
                                page = args.page + 1
                            )
                        )
                    }

                }else{
                    snackbar(R.string.id_wrong_choice_check_your)
                    findNavController().popBackStack(R.id.recoveryIntroFragment, false)
                }
            }
        }

        binding.clickListener = View.OnClickListener { button ->
            viewModel.selectWord((button as Button).text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        setSecureScreen(true)
    }

    override fun getWalletViewModel(): AbstractWalletViewModel? = null
}
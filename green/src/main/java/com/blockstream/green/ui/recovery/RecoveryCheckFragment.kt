package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryCheckFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.snackbar
import com.greenaddress.Bridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryCheckFragment : AppFragment<RecoveryCheckFragmentBinding>(
    layout = R.layout.recovery_check_fragment,
    menuRes = 0
) {
    @Inject
    lateinit var viewModelFactory: RecoveryCheckViewModel.AssistedFactory

    private val viewModel: RecoveryCheckViewModel by viewModels {
        RecoveryCheckViewModel.provideFactory(
            viewModelFactory,
            args.mnemonic.split(" "),
            args.page,
            requireContext().isDevelopmentFlavor()
        )
    }

    private val args: RecoveryCheckFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.isDevelopmentFlavor = requireContext().isDevelopmentFlavor()

        binding.clickListener = View.OnClickListener { button ->
            if (viewModel.selectWord((button as Button).text.toString())) {
                if (viewModel.isLastPage) {
                    navigate(
                        RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentToWalletNameFragment(
                            onboardingOptions = args.onboardingOptions!!,
                            mnemonic = args.mnemonic!!,
                            mnemonicPassword = ""
                        )
                    )
                } else {
                    navigate(
                        RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentSelf(
                            onboardingOptions = args.onboardingOptions,
                            mnemonic = args.mnemonic,
                            page = args.page + 1
                        )
                    )
                }
            } else {
                snackbar(R.string.id_wrong_choice_check_your)
                findNavController().popBackStack(R.id.recoveryIntroFragment, false)
            }
        }
    }
}
package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoverySetupWordsFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecoveryWordsFragment : AppFragment<RecoverySetupWordsFragmentBinding>(
    layout = R.layout.recovery_setup_words_fragment,
    menuRes = 0
) {
    private val args: RecoveryWordsFragmentArgs by navArgs()

    private val viewModel: RecoveryWordsViewModel by viewModels {

        RecoveryWordsViewModel.provideFactory(
            args.mnemonic.split(" "),
            args.page
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonNext.setOnClickListener {

            val nextPage = args.page + 1

            if (viewModel.isLastPage) {
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentToRecoveryCheckFragment(
                        onboardingOptions = args.onboardingOptions,
                        mnemonic = args.mnemonic
                    )
                )
            } else {
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentSelf(
                        onboardingOptions = args.onboardingOptions,
                        mnemonic = args.mnemonic,
                        page = nextPage
                    )
                )
            }
        }
    }
}
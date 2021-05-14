package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoverySetupWordsFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.wallet.WalletViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecoveryWordsFragment : WalletFragment<RecoverySetupWordsFragmentBinding>(
    layout = R.layout.recovery_setup_words_fragment,
    menuRes = 0
) {
    private val args: RecoveryWordsFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet!! }

    private val viewModel: RecoveryWordsViewModel by viewModels {
        val mnemonic = args.mnemonic ?: session.getMnemonicPassphrase()

        RecoveryWordsViewModel.provideFactory(
            mnemonic.split(" "),
            args.page
        )
    }

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionRequired(): Boolean {
        return args.wallet != null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonNext.setOnClickListener {

            val nextPage = args.page + 1

            if (viewModel.isLastPage) {
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentToRecoveryCheckFragment(
                        wallet = args.wallet,
                        onboardingOptions = args.onboardingOptions,
                        mnemonic = args.mnemonic
                    )
                )
            } else {
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentSelf(
                        wallet = args.wallet,
                        onboardingOptions = args.onboardingOptions,
                        mnemonic = args.mnemonic,
                        page = nextPage
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setSecureScreen(true)
    }

    override fun getWalletViewModel(): WalletViewModel? = null
}
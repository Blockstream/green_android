package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoverySetupWordsFragmentBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.greenText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryWordsFragment : AbstractWalletFragment<RecoverySetupWordsFragmentBinding>(
    layout = R.layout.recovery_setup_words_fragment,
    menuRes = 0
) {
    private val args: RecoveryWordsFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }
    private val networkOrNull by lazy { args.network }

    override val screenName = "RecoveryWords"

    override val title: String
        get() = networkOrNull?.canonicalName ?: ""

    override val toolbarIcon: Int?
        get() = networkOrNull?.getNetworkIcon()

    @Inject
    lateinit var viewModelFactory: RecoveryWordsViewModel.AssistedFactory

    private val viewModel: RecoveryWordsViewModel by viewModels {
        RecoveryWordsViewModel.provideFactory(
            viewModelFactory,
            args.mnemonic.split(" "),
            args.page
        )
    }

    @Inject
    lateinit var walletViewModelFactory: WalletViewModel.AssistedFactory
    val walletViewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(walletViewModelFactory, args.wallet!!)
    }

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getAppViewModel() : AppViewModel? = if(args.wallet == null) null else getWalletViewModel()

    override fun getWalletViewModel(): AbstractWalletViewModel = if(args.wallet != null) walletViewModel else throw RuntimeException("Can't be happening")

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionAndWalletRequired(): Boolean {
        return args.wallet != null
    }

    override fun isLoggedInRequired(): Boolean = isSessionAndWalletRequired()

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        binding.title.text = greenText(R.string.id_write_down_your_recovery_phrase, R.string.id_recovery_phrase, R.string.id_correct_order)

        binding.buttonNext.setOnClickListener {

            val nextPage = args.page + 1

            if (viewModel.isLastPage) {
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentToRecoveryCheckFragment(
                        wallet = args.wallet,
                        assetId = args.assetId,
                        onboardingOptions = args.onboardingOptions,
                        mnemonic = args.mnemonic,
                        network = args.network
                    )
                )
            } else {
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentSelf(
                        wallet = args.wallet,
                        assetId = args.assetId,
                        onboardingOptions = args.onboardingOptions,
                        mnemonic = args.mnemonic,
                        page = nextPage,
                        network = args.network
                    )
                )
            }
        }
    }
}
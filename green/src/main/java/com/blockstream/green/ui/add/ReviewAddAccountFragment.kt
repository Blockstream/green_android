package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.ReviewAddAccountFragmentBinding
import com.blockstream.green.gdk.titleRes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewAddAccountFragment : AbstractAddAccountFragment<ReviewAddAccountFragmentBinding>(
    layout = R.layout.review_add_account_fragment,
    menuRes = 0
) {
    val args: ReviewAddAccountFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "AddAccountConfirm"

    override val addAccountViewModel: AbstractAddAccountViewModel
        get() = viewModel

    override val network by lazy { gdkBridge.networks.getNetworkByAccountType(args.network.id, args.accountType) }

    override val assetId: String
        get() = args.assetId

    @Inject
    lateinit var viewModelFactory: ReviewAddAccountViewModel.AssistedFactory
    val viewModel: ReviewAddAccountViewModel by viewModels {
        ReviewAddAccountViewModel.provideFactory(
            viewModelFactory,
            args.wallet,
            network,
            args.accountType,
            args.mnemonic,
            args.xpub
        )
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedGuarded(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonContinue.setOnClickListener {
            viewModel.createAccount(
                accountType = args.accountType,
                accountName = getString(args.accountType.titleRes()),
                network = network,
                args.mnemonic,
                args.xpub
            )
        }
    }
}
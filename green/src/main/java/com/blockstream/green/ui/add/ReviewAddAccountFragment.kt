package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.ReviewAddAccountFragmentBinding
import com.blockstream.green.gdk.titleRes
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class ReviewAddAccountFragment : AbstractAddAccountFragment<ReviewAddAccountFragmentBinding>(
    layout = R.layout.review_add_account_fragment,
    menuRes = 0
) {
    val args: ReviewAddAccountFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.setupArgs.greenWallet }

    override val screenName = "AddAccountConfirm"

    override val addAccountViewModel: AbstractAddAccountViewModel
        get() = viewModel

    override val network by lazy {
        args.setupArgs.network!!
        // gdk.networks().getNetworkByAccountType(args.network.id, args.accountType)
    }

    override val assetId: String
        get() = args.setupArgs.assetId!!

    val viewModel: ReviewAddAccountViewModel by viewModel {
        parametersOf(
            args.setupArgs,
        )
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedGuarded(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonContinue.setOnClickListener {
            viewModel.createAccount(
                accountType = args.setupArgs.accountType!!,
                accountName = getString(args.setupArgs.accountType.titleRes()),
                network = args.setupArgs.network!!,
                mnemonic = args.setupArgs.mnemonic,
                xpub = args.setupArgs.xpub
            )
        }
    }
}
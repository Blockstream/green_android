package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.add.ReviewAddAccountViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.ReviewAddAccountFragmentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class ReviewAddAccountFragment : AbstractAddAccountFragment<ReviewAddAccountFragmentBinding>(
    layout = R.layout.review_add_account_fragment,
    menuRes = 0
) {
    val args: ReviewAddAccountFragmentArgs by navArgs()

    override val network
        get() = args.setupArgs.network

    override val assetId: String?
        get() = args.setupArgs.assetId

    override val viewModel: ReviewAddAccountViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.buttonContinue.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }
    }
}
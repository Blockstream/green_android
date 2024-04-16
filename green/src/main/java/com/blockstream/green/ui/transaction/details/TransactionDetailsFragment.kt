package com.blockstream.green.ui.transaction.details

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.transaction.TransactionScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class TransactionDetailsFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view
) {
    val args: TransactionDetailsFragmentArgs by navArgs()

    val viewModel: TransactionViewModel by viewModel {
        parametersOf(args.transaction, args.wallet)
    }

    override val useCompose: Boolean = true

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    TransactionScreen(viewModel = viewModel)
                }
            }
        }
    }
}
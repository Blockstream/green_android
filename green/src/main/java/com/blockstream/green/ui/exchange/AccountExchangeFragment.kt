package com.blockstream.green.ui.exchange

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.exchange.AccountExchangeViewModel
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.exchange.AccountExchangeScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AccountExchangeFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = 0
) {
    val args: AccountExchangeFragmentArgs by navArgs()

    val viewModel: AccountExchangeViewModel by viewModel {
        parametersOf(
            args.wallet,
            null
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val title: String
        get() = getString(R.string.id_account_transfer)

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    AccountExchangeScreen(viewModel = viewModel)
                }
            }
        }
    }
}

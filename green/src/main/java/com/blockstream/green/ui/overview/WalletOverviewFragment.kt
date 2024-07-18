package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.utils.Loggable
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.AppFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class WalletOverviewFragment : AppFragment<ComposeViewBinding>(
    layout = R.layout.compose_view,
    menuRes = R.menu.wallet_overview
) {
    val args: WalletOverviewFragmentArgs by navArgs()

    val viewModel: WalletOverviewViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    WalletOverviewScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.options -> {
                viewModel.postEvent(WalletOverviewViewModel.LocalEvents.OpenOptionsMenu)
            }
        }

        return super.onMenuItemSelected(menuItem)
    }

    companion object : Loggable()
}
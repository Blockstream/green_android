package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
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
){

    val args: WalletOverviewFragmentArgs by navArgs()

    val viewModel: WalletOverviewViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel


    // Prevent ViewModel initialization if session is not initialized
    override val title: String
        get() = viewModel.greenWallet.name

    override val subtitle: String?
        get() = if(viewModel.sessionOrNull?.isLightningShortcut == true) getString(R.string.id_lightning_account) else null

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

    override fun onPrepareMenu(menu: Menu) {
        // Prevent from archiving all your acocunts
        menu.findItem(R.id.create_account).isVisible = !viewModel.session.isWatchOnly && !viewModel.greenWallet.isLightning
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.settings -> {
                navigate(
                    WalletOverviewFragmentDirections.actionWalletOverviewFragmentToWalletSettingsFragment(
                        viewModel.greenWallet, false
                    )
                )
                return true
            }
            R.id.create_account -> {
                navigate(
                    WalletOverviewFragmentDirections.actionGlobalChooseAccountTypeFragment(
                        wallet = viewModel.greenWallet,
                        isReceive = false
                    )
                )
                countly.accountNew(viewModel.session)
            }
            R.id.logout -> {
                viewModel.postEvent(Events.Logout(LogoutReason.USER_ACTION))
            }
        }

        return super.onMenuItemSelected(menuItem)
    }
}
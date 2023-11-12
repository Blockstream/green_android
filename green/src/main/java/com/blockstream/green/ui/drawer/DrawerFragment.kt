package com.blockstream.green.ui.drawer

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockstream.common.Urls
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.compose.screens.DrawerScreen
import com.blockstream.compose.screens.DrawerScreenCallbacks
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.HomeScreenCallbacks
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.bottomsheets.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import com.blockstream.green.utils.openBrowser
import org.koin.androidx.viewmodel.ext.android.viewModel


class DrawerFragment : AbstractWalletsFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {

    private val viewModel: DrawerViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                GreenTheme {
                    DrawerScreen(viewModel = viewModel, callbacks = DrawerScreenCallbacks(
                        onWalletClick = { wallet, isLightningShortcut ->
                            closeDrawer()
                            viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(greenWallet = wallet, isLightningShortcut = isLightningShortcut))
                        },
                        onNewWalletClick = {
                            closeDrawer()
                            navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
                        },
                        onHelpClick = {
                            closeDrawer()
                            openBrowser(Urls.HELP_CENTER)
                        },
                        onAboutClick = {
                            closeDrawer()
                            navigate(NavGraphDirections.actionGlobalAboutFragment())
                        },
                        onAppSettingsClick = {
                            closeDrawer()
                            navigate(NavGraphDirections.actionGlobalAppSettingsFragment())
                        }
                    ))
                }
            }
        }
    }
}
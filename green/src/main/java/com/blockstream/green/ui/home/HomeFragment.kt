package com.blockstream.green.ui.home

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.Urls
import com.blockstream.common.models.home.HomeViewModel
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment :
    AbstractWalletsFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {
    val viewModel: HomeViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        combine(
            viewModel.softwareWallets,
            viewModel.ephemeralWallets,
            viewModel.hardwareWallets
        ) { w1, w2, w3 ->
            w1?.isEmpty() == true && w2?.isEmpty() == true && w3?.isEmpty() == true
        }.onEach {
            if (it) {
                navigate(NavGraphDirections.actionGlobalIntroSetupNewWalletFragment())
            }
        }.launchIn(lifecycleScope)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                GreenTheme {
                    HomeScreen(viewModel = viewModel, callbacks = HomeScreenCallbacks(
                        onWalletClick = { wallet, isLightningShortcut ->
                            closeDrawer()
                            viewModel.postEvent(WalletsViewModel.LocalEvents.SelectWallet(greenWallet = wallet, isLightningShortcut = isLightningShortcut))
                        },
                        onWalletRename = {
                            RenameWalletBottomSheetDialogFragment.show(
                                it,
                                childFragmentManager
                            )
                        },
                        onWalletDelete = {
                            DeleteWalletBottomSheetDialogFragment.show(
                                it,
                                childFragmentManager
                            )
                        },
                        onLightningShortcutDelete = {
                             viewModel.postEvent(WalletsViewModel.LocalEvents.RemoveLightningShortcut(it))
                        },
                        onNewWalletClick = {
                            closeDrawer()
                            navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
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
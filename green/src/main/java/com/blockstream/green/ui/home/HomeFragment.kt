package com.blockstream.green.ui.home

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.HomeScreenCallbacks
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.ui.wallet.AbstractWalletsFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment :
    AbstractWalletsFragment<ComposeViewBinding>(R.layout.compose_view, menuRes = 0) {
    val viewModel: HomeViewModel by viewModel()

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true

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
                    BottomSheetNavigatorM3 {
                        HomeScreen(viewModel = viewModel, callbacks = HomeScreenCallbacks(
                            onWalletClick = { wallet, isLightningShortcut ->
                                closeDrawer()
                                viewModel.postEvent(
                                    WalletsViewModel.LocalEvents.SelectWallet(
                                        greenWallet = wallet,
                                        isLightningShortcut = isLightningShortcut
                                    )
                                )
                            },
                            onNewWalletClick = {
                                closeDrawer()
                                navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
                            },
                            onLightningShortcutDelete = {
                                viewModel.postEvent(WalletsViewModel.LocalEvents.RemoveLightningShortcut(it))
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
}
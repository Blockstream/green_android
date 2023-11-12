package com.blockstream.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.AboutButton
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.views.BannerView

class HomeScreenCallbacks(
    onWalletClick: (wallet: GreenWallet, isLightning: Boolean) -> Unit = { _, _ -> },
    onLightningShortcutDelete: ((wallet: GreenWallet) -> Unit)? = { },
    onNewWalletClick: () -> Unit = {},
    val onAboutClick: () -> Unit = {},
    val onAppSettingsClick: () -> Unit = {},
) : WalletSectionCallbacks(onWalletClick = onWalletClick, onLightningShortcutDelete = onLightningShortcutDelete, onNewWalletClick = onNewWalletClick, hasContextMenu = true)

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<HomeViewModel>()

        AppBar()

        HomeScreen(viewModel = viewModel, callbacks = HomeScreenCallbacks(
            onWalletClick = { wallet, isLightningShortcut ->
                viewModel.postEvent(
                    WalletsViewModel.LocalEvents.SelectWallet(
                        greenWallet = wallet,
                        isLightningShortcut = isLightningShortcut
                    )
                )
            },
            onNewWalletClick = {
                // navigate(NavGraphDirections.actionGlobalSetupNewWalletFragment())
            }
        ))
    }
}

@Composable
fun HomeScreen(
    viewModel: WalletsViewModelAbstract,
    callbacks: HomeScreenCallbacks = HomeScreenCallbacks()
) {
    HandleSideEffect(viewModel = viewModel)

    Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Image(
            painter = painterResource(id = R.drawable.brand),
            contentDescription = null,
            modifier = Modifier
                .heightIn(50.dp, 70.dp)
                .align(Alignment.CenterHorizontally)
        )

        BannerView(viewModel)

        WalletsScreen(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            viewModel = viewModel,
            callbacks = callbacks
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AboutButton {
                callbacks.onAboutClick()
                viewModel.postEvent(Events.About)
            }

            AppSettingsButton {
                callbacks.onAppSettingsClick()
                viewModel.postEvent(Events.AppSettings)
            }
        }
    }
}

@Composable
@Preview
fun HomeScreenPreview(){
    GreenTheme {
        BottomSheetNavigatorM3 {
            HomeScreen(viewModel = WalletsViewModelPreview.previewSoftwareOnly())
        }
    }
}
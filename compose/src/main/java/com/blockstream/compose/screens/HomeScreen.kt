package com.blockstream.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.AboutButton
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.views.BannerView

class HomeScreenCallbacks(
    onWalletClick: (wallet: GreenWallet, isLightning: Boolean) -> Unit = { _, _ -> },
    onWalletRename: (wallet: GreenWallet) -> Unit = { },
    onWalletDelete: (wallet: GreenWallet) -> Unit = { },
    onNewWalletClick: () -> Unit = {},
    val onAboutClick: () -> Unit = {},
    val onAppSettingsClick: () -> Unit = {},
) : WalletSectionCallbacks(onWalletClick = onWalletClick, onWalletRename = onWalletRename , onWalletDelete = onWalletDelete, onNewWalletClick = onNewWalletClick)

@Composable
fun HomeScreen(
    viewModel: WalletsViewModelAbstract,
    callbacks: HomeScreenCallbacks = HomeScreenCallbacks()
) {

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

        Row(modifier = Modifier.fillMaxWidth()) {
            AboutButton {
                callbacks.onAboutClick()
            }

            Spacer(modifier = Modifier.weight(1f))

            AppSettingsButton {
                callbacks.onAppSettingsClick()
            }
        }
    }
}

@Composable
@Preview
fun HomeScreenPreview(){
    GreenTheme {
        HomeScreen(viewModel = WalletsViewModelPreview.previewSoftwareOnly())
    }
}
package com.blockstream.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.HelpButton
import com.blockstream.compose.theme.GreenTheme


class DrawerScreenCallbacks(
    onWalletClick: (wallet: GreenWallet, isLightning: Boolean) -> Unit = { _, _ -> },
    onNewWalletClick: () -> Unit = {},
    val onHelpClick: () -> Unit = {},
    val onAboutClick: () -> Unit = {},
    val onAppSettingsClick: () -> Unit = {},
) : WalletSectionCallbacks(onWalletClick = onWalletClick, onNewWalletClick = onNewWalletClick)

@Composable
fun DrawerScreen(
    viewModel: WalletsViewModelAbstract,
    callbacks: DrawerScreenCallbacks = DrawerScreenCallbacks()
) {

    Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Image(
            painter = painterResource(id = R.drawable.brand),
            contentDescription = null,
            modifier = Modifier
                .height(40.dp)
        )

        GreenSpacer(24)

        WalletsScreen(
            modifier = Modifier.weight(1f),
            viewModel = viewModel,
            callbacks = callbacks,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            HelpButton {
                callbacks.onHelpClick()
            }
        }

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
fun DrawerScreenPreview() {
    GreenTheme {
        DrawerScreen(viewModel = WalletsViewModelPreview.previewSoftwareOnly())
    }
}
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
import com.blockstream.common.events.Events
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalDrawer
import com.blockstream.compose.R
import com.blockstream.compose.components.AboutButton
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.HelpButton
import com.blockstream.compose.utils.HandleSideEffect
import kotlinx.coroutines.launch


@Composable
fun DrawerScreen(
    viewModel: WalletsViewModelAbstract
) {
    val drawer = LocalDrawer.current
    HandleSideEffect(viewModel = viewModel) {
        launch {
            drawer.close()
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Image(
            painter = painterResource(id = R.drawable.brand),
            contentDescription = null,
            modifier = Modifier
                .height(40.dp)
        )

        GreenSpacer(16)

        WalletsScreen(
            modifier = Modifier.weight(1f),
            viewModel = viewModel
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            HelpButton {
                viewModel.postEvent(DrawerViewModel.LocalEvents.ClickHelp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            AboutButton {
                viewModel.postEvent(NavigateDestinations.About)
            }

            Spacer(modifier = Modifier.weight(1f))

            AppSettingsButton {
                viewModel.postEvent(NavigateDestinations.AppSettings)
            }
        }
    }
}

@Composable
@Preview
fun DrawerScreenPreview() {
    GreenPreview {
        DrawerScreen(viewModel = WalletsViewModelPreview.previewSoftwareOnly())
    }
}
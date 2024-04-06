package com.blockstream.compose.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.blockstream.common.events.Events
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelAbstract
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenArrow
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.roundBackground


object SetupNewWalletScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<SetupNewWalletViewModel>()

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        SetupNewWalletScreen(viewModel = viewModel)
    }
}

@Composable
fun KeysPolicyListItem(
    title: String,
    message: String,
    tag: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier
                .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                .clickable {
                    onClick()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
            ) {
                GreenColumn(padding = 0, space = 8, modifier = Modifier.padding(end = 80.dp)) {
                    Text(text = title, style = titleMedium)
                    Text(
                        text = message,
                        color = whiteMedium
                    )
                    Text(
                        text = tag,
                        style = labelLarge,
                        modifier = Modifier.roundBackground(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                GreenArrow(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Image(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp),
            painter = painter,
            contentDescription = ""
        )
    }
}

@Composable
fun SetupNewWalletScreen(
    viewModel: SetupNewWalletViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.id_how_do_you_want_to_secure_your_funds),
                style = displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )

            KeysPolicyListItem(
                title = stringResource(id = R.string.id_on_this_device),
                message = stringResource(id = R.string.id_your_phone_will_store_the_keys_to_your),
                tag = stringResource(id = R.string.id_for_ease_of_use),
                painter = painterResource(id = R.drawable.keys_device)
            ) {
                viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickOnThisDevice)
            }

            KeysPolicyListItem(
                title = stringResource(id = R.string.id_on_hardware_wallet),
                message = stringResource(id = R.string.id_your_keys_will_be_secured_on_a_dedicated_cold_storage),
                tag = stringResource(id = R.string.id_for_higher_security),
                painter = painterResource(id = R.drawable.keys_hardware)
            ) {
                viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.ClickOnHardwareWallet)
            }
        }


        GreenButton(
            stringResource(R.string.id_watchonly),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            size = GreenButtonSize.BIG,
            type = GreenButtonType.OUTLINE,
            color = GreenButtonColor.WHITE,
        ) {
            viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.WatchOnly)
        }

        AppSettingsButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp)
        ) {
            viewModel.postEvent(Events.AppSettings)
        }
    }

}

@Composable
@Preview
fun SetupNewWalletScreenPreview() {
    GreenPreview {
        SetupNewWalletScreen(viewModel = SetupNewWalletViewModelPreview.preview())
    }
}
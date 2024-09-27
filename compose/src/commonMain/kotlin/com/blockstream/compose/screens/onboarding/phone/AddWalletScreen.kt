package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.hw_matrix_bg
import blockstream_green.common.generated.resources.id_new_wallet
import blockstream_green.common.generated.resources.id_restore_wallet
import blockstream_green.common.generated.resources.id_take_control_your_keys_your
import blockstream_green.common.generated.resources.id_your_keys_secure_your_coins_on
import blockstream_green.common.generated.resources.phone_keys
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.sheets.EnvironmentBottomSheet
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


object AddWalletScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AddWalletViewModel>()

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        AddWalletScreen(viewModel = viewModel)
    }
}

@Composable
fun AddWalletScreen(
    viewModel: AddWalletViewModelAbstract
) {

    EnvironmentBottomSheet.getResult {
        if(it >= 0) {
            viewModel.postEvent(
                AddWalletViewModel.LocalEvents.SelectEnviroment(
                    isTestnet = it == 1,
                    customNetwork = null
                )
            )
        }
    }

    HandleSideEffect(viewModel = viewModel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Box(
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 24.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.hw_matrix_bg),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )

            Image(
                painter = painterResource(Res.drawable.phone_keys),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        GreenColumn(
            space = 8,
            modifier = Modifier.padding(horizontal = 0.dp).weight(4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.id_take_control_your_keys_your),
                style = displayMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(Res.string.id_your_keys_secure_your_coins_on),
                textAlign = TextAlign.Center,
                color = whiteMedium
            )
        }

        Column {
            GreenColumn(
                padding = 24,
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                GreenButton(
                    stringResource(Res.string.id_new_wallet),
                    modifier = Modifier.fillMaxWidth(),
                    size = GreenButtonSize.BIG,
                ) {
                    viewModel.postEvent(AddWalletViewModel.LocalEvents.NewWallet)
                }

                GreenButton(
                    stringResource(Res.string.id_restore_wallet),
                    modifier = Modifier.fillMaxWidth(),
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE,
                    size = GreenButtonSize.BIG,
                ) {
                    viewModel.postEvent(AddWalletViewModel.LocalEvents.RestoreWallet)
                }

            }
        }
    }

}
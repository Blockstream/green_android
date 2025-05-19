package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_new_wallet
import blockstream_green.common.generated.resources.id_restore_wallet
import blockstream_green.common.generated.resources.id_take_control_your_keys_your
import blockstream_green.common.generated.resources.id_your_keys_secure_your_coins_on
import blockstream_green.common.generated.resources.phone_keys
import com.blockstream.common.models.onboarding.phone.AddWalletViewModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddWalletScreen(
    viewModel: AddWalletViewModelAbstract
) {

    NavigateDestinations.Environment.getResult<Int> {
        if (it >= 0) {
            viewModel.postEvent(
                AddWalletViewModel.LocalEvents.SelectEnviroment(
                    isTestnet = it == 1,
                    customNetwork = null
                )
            )
        }
    }

    SetupScreen(viewModel = viewModel, withPadding = false, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 24.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.phone_keys),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        GreenColumn(
            space = 8,
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

        Spacer(modifier = Modifier.weight(1f))

        GreenColumn(
            padding = 0,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
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
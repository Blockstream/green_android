package com.blockstream.compose.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_create_a_new_wallet_to_begin
import blockstream_green.common.generated.resources.id_restore_from_backup
import blockstream_green.common.generated.resources.id_setup_mobile_wallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.stringResource

@Composable
fun SetupNewWalletScreen(
    viewModel: SetupNewWalletViewModelAbstract
) {
    NavigateDestinations.Environment.getResult<Int> {
        if (it >= 0) {
            viewModel.postEvent(
                Events.SelectEnviroment(
                    isTestnet = it == 1,
                    customNetwork = null
                )
            )
        }
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = true,
        verticalArrangement = Arrangement.SpaceAround,
        onProgressStyle = OnProgressStyle.Full(
            bluBackground = false,
            riveAnimation = RiveAnimation.ROCKET
        )
    ) {
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Rive(
                riveAnimation = RiveAnimation.CREATE_WALLET
            )

            Text(
                text = stringResource(Res.string.id_create_a_new_wallet_to_begin),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = whiteMedium
            )
        }

        GreenColumn(
            padding = 0
        ) {

            GreenButton(
                text = stringResource(Res.string.id_setup_mobile_wallet),
                size = GreenButtonSize.BIG,
                enabled = !onProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.onSetupNewWallet()
            }

            GreenButton(
                text = stringResource(Res.string.id_restore_from_backup),
                type = GreenButtonType.TEXT,
                size = GreenButtonSize.BIG,
                enabled = !onProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.postEvent(SetupNewWalletViewModel.LocalEvents.RestoreWallet)
            }
        }
    }
}

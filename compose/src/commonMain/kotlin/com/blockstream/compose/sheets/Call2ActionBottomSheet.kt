package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_2fa_isnt_set_up_yetnnyou_can
import blockstream_green.common.generated.resources.id_enable_2fa
import blockstream_green.common.generated.resources.id_setup_2fa_now
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Call2ActionBottomSheet(
    viewModel: GreenViewModel,
    network: Network,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_enable_2fa),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        HandleSideEffect(viewModel = viewModel)

        Text(
            text = stringResource(Res.string.id_2fa_isnt_set_up_yetnnyou_can),
            textAlign = TextAlign.Center
        )

        GreenButton(
            text = stringResource(Res.string.id_setup_2fa_now),
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(NavigateDestinations.TwoFactorAuthentication(greenWallet = viewModel.greenWallet, network = network))
            onDismissRequest()
        }
    }
}

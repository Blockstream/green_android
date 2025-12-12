package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_be_sure_your_recovery_phrase_is
import blockstream_green.common.generated.resources.id_do_you_have_the_backup
import blockstream_green.common.generated.resources.id_remove_wallet
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.wallet.WalletDeleteViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.labelLarge
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDeleteBottomSheet(
    viewModel: WalletDeleteViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_remove_wallet),
        subtitle = viewModel.greenWallet.name,
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        Text(text = stringResource(Res.string.id_do_you_have_the_backup), style = labelLarge)
        Text(text = stringResource(Res.string.id_be_sure_your_recovery_phrase_is))

        var isConfirmed by remember {
            mutableStateOf(false)
        }

        if (isConfirmed) {
            GreenButton(
                text = stringResource(Res.string.id_remove_wallet),
                modifier = Modifier.fillMaxWidth(),
                color = GreenButtonColor.RED,
                testTag = "remove_wallet"
            ) {
                viewModel.postEvent(Events.Continue)
            }
        } else {
            GreenButton(
                text = stringResource(Res.string.id_remove_wallet),
                modifier = Modifier.fillMaxWidth(),
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.RED,
                testTag = "remove_wallet"
            ) {
                isConfirmed = true
            }
        }
    }
}

package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_sign_transaction_via_qr
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenConfirmButton(viewModel: CreateTransactionViewModelAbstract, onConfirm: (isWatchOnly: Boolean) -> Unit) {

    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
    val isWatchOnly by viewModel.isWatchOnly.collectAsStateWithLifecycle()

    if (isWatchOnly) {
        GreenButton(
            text = stringResource(Res.string.id_sign_transaction_via_qr),
            enabled = buttonEnabled,
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onConfirm.invoke(true)
            }
        )
    } else {
        SlideToUnlock(
            modifier = Modifier.padding(top = 8.dp),
            isLoading = onProgressSending,
            enabled = buttonEnabled,
            onSlideComplete = {
                onConfirm.invoke(false)
            }
        )
    }
}
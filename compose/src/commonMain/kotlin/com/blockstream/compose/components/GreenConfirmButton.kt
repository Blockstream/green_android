package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_connect_hardware_wallet
import blockstream_green.common.generated.resources.id_sign_transaction
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.PendingAction
import com.blockstream.common.models.send.SendConfirmViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenConfirmButton(
    viewModel: CreateTransactionViewModelAbstract,
    isSweep: Boolean = false,
    onConfirm: (isWatchOnly: Boolean) -> Unit
) {
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
    val isWatchOnly by viewModel.isWatchOnly.collectAsStateWithLifecycle()
    val isHwWatchOnly by viewModel.isHwWatchOnly.collectAsStateWithLifecycle()
    val isQrWatchOnly by viewModel.isQrWatchOnly.collectAsStateWithLifecycle()

    if (isWatchOnly && !isSweep) {
        if (viewModel.account.let { !it.isLiquid && !it.isMultisig }) {
            GreenButton(
                text = stringResource(Res.string.id_sign_transaction),
                enabled = buttonEnabled,
                size = GreenButtonSize.BIG,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onConfirm.invoke(true)
                }
            )
        }

        if (isHwWatchOnly && !isQrWatchOnly) {
            GreenButton(
                text = stringResource(Res.string.id_connect_hardware_wallet),
                enabled = buttonEnabled,
                type = GreenButtonType.OUTLINE,
                size = GreenButtonSize.BIG,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.pendingAction = PendingAction.SendTransaction
                    viewModel.postEvent(NavigateDestinations.DeviceScan(greenWallet = viewModel.greenWallet, isWatchOnlyUpgrade = true))
                }
            )
        }
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

@Preview
@Composable
fun GreenConfirmButtonPreview() {
    GreenPreview {
        GreenConfirmButton(SendConfirmViewModelPreview.preview()) {

        }
    }
}
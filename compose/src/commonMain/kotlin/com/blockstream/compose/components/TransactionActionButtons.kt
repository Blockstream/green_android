package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_buy
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_send
import blockstream_green.common.generated.resources.id_swap
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLineDown
import com.adamglin.phosphoricons.regular.ArrowLineUp
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.adamglin.phosphoricons.regular.ShoppingCart
import com.blockstream.compose.GreenPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TransactionActionButtons(
    modifier: Modifier = Modifier,
    showBuyButton: Boolean,
    showSwapButton: Boolean,
    sendEnabled: Boolean,
    onBuy: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onSwap: () -> Unit,
) {
    GreenRow(
        space = 8,
        padding = 0,
        modifier = modifier
    ) {
        if (showBuyButton) {
            GreenActionButton(
                text = Res.string.id_buy,
                icon = PhosphorIcons.Regular.ShoppingCart,
                onClick = onBuy
            )
        }

        GreenActionButton(
            text = Res.string.id_send,
            icon = PhosphorIcons.Regular.ArrowLineUp,
            enabled = sendEnabled,
            onClick = onSend
        )

        GreenActionButton(
            text = Res.string.id_receive,
            icon = PhosphorIcons.Regular.ArrowLineDown,
            onClick = onReceive
        )

        if (showSwapButton) {
            GreenActionButton(
                text = Res.string.id_swap,
                icon = PhosphorIcons.Regular.ArrowsDownUp,
                onClick = onSwap
            )
        }
    }
}

@Composable
@Preview
fun TransactionActionButtonsPreview() {
    GreenPreview {
        TransactionActionButtons(
            showBuyButton = true,
            showSwapButton = true,
            sendEnabled = true,
            onBuy = { },
            onSend = { },
            onReceive = { },
            onSwap = { }
        )
    }
}

package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bitcoin
import blockstream_green.common.generated.resources.bitcoin_lightning
import blockstream_green.common.generated.resources.bitcoin_lightning_testnet
import blockstream_green.common.generated.resources.bitcoin_testnet
import blockstream_green.common.generated.resources.liquid
import blockstream_green.common.generated.resources.liquid_testnet
import com.blockstream.compose.looks.transaction.Completed
import com.blockstream.compose.looks.transaction.Failed
import com.blockstream.compose.looks.transaction.Unconfirmed
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.resources.painterResource

@Preview
@Composable
fun TransactionStatusIconPreview() {
    GreenChromePreview {
        Column {

            TransactionStatusIcon(
                transactionStatus = Completed(),
                icons = listOf(painterResource(Res.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Completed(),
                icons = listOf(painterResource(Res.drawable.bitcoin), painterResource(Res.drawable.liquid)),
                isSwap = true
            )
            TransactionStatusIcon(
                transactionStatus = Unconfirmed(),
                icons = listOf(painterResource(Res.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(painterResource(Res.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(
                    painterResource(Res.drawable.bitcoin),
                    painterResource(Res.drawable.liquid),
                )
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(
                    painterResource(Res.drawable.bitcoin),
                    painterResource(Res.drawable.liquid),
                    painterResource(Res.drawable.bitcoin_lightning),
                    painterResource(Res.drawable.bitcoin_testnet),
                    painterResource(Res.drawable.liquid_testnet),
                    painterResource(Res.drawable.bitcoin_lightning_testnet)
                )
            )
        }
    }
}
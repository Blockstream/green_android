package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.looks.transaction.Completed
import com.blockstream.common.looks.transaction.Failed
import com.blockstream.common.looks.transaction.Unconfirmed
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview


@Preview
@Composable
fun TransactionStatusIconPreview() {
    GreenThemePreview {
        Column {

            TransactionStatusIcon(
                transactionStatus = Completed(),
                icons = listOf(painterResource(id = R.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Completed(),
                icons = listOf(painterResource(id = R.drawable.bitcoin), painterResource(id = R.drawable.liquid)),
                isSwap = true
            )
            TransactionStatusIcon(
                transactionStatus = Unconfirmed(),
                icons = listOf(painterResource(id = R.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(painterResource(id = R.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(
                    painterResource(id = R.drawable.bitcoin),
                    painterResource(id = R.drawable.liquid),
                )
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(
                    painterResource(id = R.drawable.bitcoin),
                    painterResource(id = R.drawable.liquid),
                    painterResource(id = R.drawable.bitcoin_lightning),
                    painterResource(id = R.drawable.bitcoin_testnet),
                    painterResource(id = R.drawable.liquid_testnet),
                    painterResource(id = R.drawable.bitcoin_lightning_testnet)
                )
            )
        }
    }
}
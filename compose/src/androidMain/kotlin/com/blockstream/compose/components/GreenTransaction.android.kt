package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.common.looks.transaction.Confirmed
import com.blockstream.compose.theme.GreenChromePreview


@Composable
@Preview
fun GreenTransactionPreview() {
    GreenChromePreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, space = 1, padding = 0) {
            GreenTransaction(transactionLook = previewTransactionLook()) {

            }

            GreenTransaction(transactionLook = previewTransactionLook(status = Confirmed(1, 2)), showAccount = false) {

            }
        }
    }
}
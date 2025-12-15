package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.previewTransactionLook
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.looks.transaction.Confirmed
import com.blockstream.compose.looks.transaction.Unconfirmed

@Composable
@Preview
fun GreenTransactionPreview() {
    GreenAndroidPreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, space = 4, padding = 0) {
            GreenTransaction(transactionLook = previewTransactionLook()) {

            }

            GreenTransaction(
                transactionLook = previewTransactionLook(status = Confirmed(1, 2)),
            ) {

            }

            GreenTransaction(
                transactionLook = previewTransactionLook(status = Unconfirmed()),
            ) {

            }
        }
    }
}
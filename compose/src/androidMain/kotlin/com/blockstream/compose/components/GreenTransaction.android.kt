package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
fun GreenTransactionPreview() {
    GreenThemePreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, space = 1, padding = 0) {
            GreenTransaction(transactionLook = previewTransactionLook()) {

            }

            GreenTransaction(transactionLook = previewTransactionLook(), showAccount = false) {

            }
        }
    }
}
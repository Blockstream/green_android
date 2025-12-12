package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.sheets.TransactionDetailsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun TransactionDetailsBottomSheetPreview() {
    GreenAndroidPreview {
        TransactionDetailsBottomSheet(
            viewModel = TransactionDetailsViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}
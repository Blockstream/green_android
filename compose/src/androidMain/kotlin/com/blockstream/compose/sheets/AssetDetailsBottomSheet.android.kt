package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.sheets.AssetDetailsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AssetDetailsBottomSheetPreview() {
    GreenAndroidPreview {
        AssetDetailsBottomSheet(
            viewModel = AssetDetailsViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}
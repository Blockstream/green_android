package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.sheets.LightningNodeViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun LightningNodeBottomSheetPreview() {
    GreenAndroidPreview {
        LightningNodeBottomSheet(
            viewModel = LightningNodeViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}
package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.sheets.AnalyticsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AnalyticsBottomSheetPreview() {
    GreenAndroidPreview {
        AnalyticsBottomSheet(
            viewModel = AnalyticsViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}
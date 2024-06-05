package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.models.sheets.AnalyticsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun Call2ActionBottomSheetPreview() {
    GreenAndroidPreview {
        Call2ActionBottomSheet(
            viewModel = AnalyticsViewModelPreview.preview(),
            network = previewNetwork(),
            onDismissRequest = { }
        )
    }
}
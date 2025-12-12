package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun QrBottomSheetPreview() {
    GreenAndroidPreview {
        QrBottomSheet(
            title = "Output Descriptors",
            subtitle = "Account #1",
            data = "This is the QR",
            viewModel = SimpleGreenViewModelPreview(),
            onDismissRequest = {}
        )
    }
}
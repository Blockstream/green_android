package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.sheets.JadeFirmwareUpdateViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun JadeFirmwareUpdateBottomSheetPreview() {
    GreenAndroidPreview {
        JadeFirmwareUpdateBottomSheet(
            viewModel = JadeFirmwareUpdateViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}
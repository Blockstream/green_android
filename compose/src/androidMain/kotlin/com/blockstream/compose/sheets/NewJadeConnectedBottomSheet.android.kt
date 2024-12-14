package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun NewJadeConnectedBottomSheetPreview() {
    GreenAndroidPreview {
        NewJadeConnectedBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            onDismissRequest = { }
        )
    }
}
package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SystemMessageBottomSheetPreview() {
    GreenAndroidPreview {
        SystemMessageBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            network = previewNetwork(),
            message = "Lorem ipsum",
            onDismissRequest = { }
        )
    }
}
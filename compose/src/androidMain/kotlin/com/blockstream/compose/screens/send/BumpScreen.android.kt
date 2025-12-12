package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.send.BumpViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun BumpScreenPreview() {
    GreenAndroidPreview {
        BumpScreen(viewModel = BumpViewModelPreview.preview())
    }
}

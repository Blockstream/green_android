package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.send.SendViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SendScreenPreview() {
    GreenAndroidPreview {
        SendScreen(viewModel = SendViewModelPreview.preview(isLightning = true))
    }
}

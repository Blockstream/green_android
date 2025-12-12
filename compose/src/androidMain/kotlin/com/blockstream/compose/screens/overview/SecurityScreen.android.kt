package com.blockstream.compose.screens.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.overview.SecurityViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Preview
@Composable
fun PreviewSecurityScreen() {
    GreenAndroidPreview {
        SecurityScreen(viewModel = SecurityViewModelPreview.preview(isHardware = true))
    }
}
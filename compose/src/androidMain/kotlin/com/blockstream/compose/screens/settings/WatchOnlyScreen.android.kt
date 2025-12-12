package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.settings.WatchOnlyViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun WatchOnlyScreenAndroidPreview() {
    GreenAndroidPreview {
        WatchOnlyScreen(viewModel = WatchOnlyViewModelPreview.preview())
    }
}
package com.blockstream.compose.screens.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.about.AboutViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AboutScreenPreview() {
    GreenAndroidPreview {
        AboutScreen(viewModel = AboutViewModelPreview.preview())
    }
}

package com.blockstream.compose.screens.support

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.support.SupportViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SupportScreenPreview() {
    GreenAndroidPreview {
        SupportScreen(viewModel = SupportViewModelPreview.preview())
    }
}
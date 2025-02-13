package com.blockstream.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.home.HomeViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun HomeScreenPreview() {
    GreenAndroidPreview {
        HomeScreen(viewModel = HomeViewModelPreview.previewEmpty())
    }
}
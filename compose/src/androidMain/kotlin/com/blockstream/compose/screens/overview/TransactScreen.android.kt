package com.blockstream.compose.screens.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.overview.TransactViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Preview
@Composable
fun PreviewTransactScreen() {
    GreenAndroidPreview {
        TransactScreen(viewModel = TransactViewModelPreview.create())
    }
}
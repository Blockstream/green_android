package com.blockstream.compose.screens.exchange

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.exchange.OnOffRampsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun OnOffRampsScreenPreview() {
    GreenAndroidPreview {
        OnOffRampsScreen(viewModel = OnOffRampsViewModelPreview.preview())
    }
}

package com.blockstream.compose.screens.exchange

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.exchange.BuyViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun BuyScreenPreview() {
    GreenAndroidPreview {
        BuyScreen(viewModel = BuyViewModelPreview.preview())
    }
}

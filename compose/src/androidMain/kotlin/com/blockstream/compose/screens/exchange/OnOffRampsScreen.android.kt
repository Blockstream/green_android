package com.blockstream.compose.screens.exchange

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.exchange.AccountExchangeViewModelPreview
import com.blockstream.common.models.exchange.OnOffRampsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun OnOffRampsScreenPreview() {
    GreenAndroidPreview {
        OnOffRampsScreen(viewModel = OnOffRampsViewModelPreview.preview())
    }
}

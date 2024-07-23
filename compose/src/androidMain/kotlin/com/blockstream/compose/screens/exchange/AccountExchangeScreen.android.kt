package com.blockstream.compose.screens.exchange

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.exchange.AccountExchangeViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun ExchangeScreenPreview() {
    GreenAndroidPreview {
        AccountExchangeScreen(viewModel = AccountExchangeViewModelPreview.preview())
    }
}

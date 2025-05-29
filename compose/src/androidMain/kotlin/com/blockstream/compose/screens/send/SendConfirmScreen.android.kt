package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.send.SendConfirmViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SendConfirmScreenPreview() {
    GreenAndroidPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.preview())
    }
}

@Composable
@Preview
fun SendConfirmScreenExchangePreview() {
    GreenAndroidPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.previewAccountExchange())
    }
}

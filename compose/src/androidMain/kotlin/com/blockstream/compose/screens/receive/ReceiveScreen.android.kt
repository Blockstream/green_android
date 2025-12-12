package com.blockstream.compose.screens.receive

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.receive.ReceiveViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun ReceiveScreenPreview() {
    GreenAndroidPreview {
        ReceiveScreen(viewModel = ReceiveViewModelPreview.preview())
    }
}
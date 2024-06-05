package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.send.RedepositViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun RedepositScreenPreview() {
    GreenAndroidPreview {
        RedepositScreen(viewModel = RedepositViewModelPreview.preview())
    }
}

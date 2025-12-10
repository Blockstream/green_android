package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.send.SendAddressViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SendAddressScreenPreview() {
    GreenAndroidPreview {
        SendAddressScreen(viewModel = SendAddressViewModelPreview.preview(isLightning = true))
    }
}

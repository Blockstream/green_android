package com.blockstream.compose.screens.jade

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.jade.JadeQRViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun QrPinUnlockScreenPreview() {
    GreenAndroidPreview {
        JadeQRScreen(JadeQRViewModelPreview.previewLightning())
    }
}
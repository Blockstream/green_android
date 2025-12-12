package com.blockstream.compose.screens.jade

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun JadePinViaQRScreenPreview() {
    GreenAndroidPreview {
        JadePinUnlockScreen(SimpleGreenViewModelPreview())
    }
}
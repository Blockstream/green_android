package com.blockstream.compose.screens.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.devices.JadeGenuineCheckViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun JadeGenuineCheckScreenPreview() {
    GreenAndroidPreview {
        JadeGenuineCheckScreen(JadeGenuineCheckViewModelPreview.preview())
    }
}
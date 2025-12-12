package com.blockstream.compose.screens.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.devices.DeviceInfoViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun DeviceInfoScreenPreview() {
    GreenAndroidPreview {
        DeviceInfoScreen(DeviceInfoViewModelPreview.preview())
    }
}
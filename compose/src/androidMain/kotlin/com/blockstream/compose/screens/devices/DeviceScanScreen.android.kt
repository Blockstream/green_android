package com.blockstream.compose.screens.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.devices.DeviceScanViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun DeviceScanScreenPreview() {
    GreenAndroidPreview {
        DeviceScanScreen(DeviceScanViewModelPreview.preview())
    }
}
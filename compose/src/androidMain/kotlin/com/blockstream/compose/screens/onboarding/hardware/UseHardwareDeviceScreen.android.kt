package com.blockstream.compose.screens.onboarding.hardware

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.onboarding.hardware.UseHardwareDeviceViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun UseHardwareDeviceScreenPreview() {
    GreenAndroidPreview {
        UseHardwareDeviceScreen(viewModel = UseHardwareDeviceViewModelPreview.preview())
    }
}
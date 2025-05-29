package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.onboarding.phone.PinViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun PinScreenPreview(
) {
    GreenAndroidPreview {
        PinScreen(viewModel = PinViewModelPreview.preview())
    }
}

@Composable
@Preview
fun PinScreenProgressPreview(
) {
    GreenAndroidPreview {
        PinScreen(viewModel = PinViewModelPreview.preview().also {
            it.onProgress.value = true
        })
    }
}
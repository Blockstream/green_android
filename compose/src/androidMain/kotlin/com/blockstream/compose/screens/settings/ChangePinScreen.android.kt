package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import com.blockstream.common.models.onboarding.phone.PinViewModelPreview
import com.blockstream.common.models.settings.WalletSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun PinScreenPreview(
) {
    GreenAndroidPreview {
        ChangePinScreen(viewModel = WalletSettingsViewModelPreview.preview())
    }
}

@Composable
@Preview
fun PinScreenProgressPreview(
) {
    GreenAndroidPreview {
        ChangePinScreen(viewModel = WalletSettingsViewModelPreview.preview().also {
             it.onProgress.value = true
        })
    }
}
package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModelPreview
import com.blockstream.common.models.settings.WalletSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun TwoFactorAuthenticationScreenPreview(
) {
    GreenAndroidPreview {
        TwoFactorAuthenticationScreen(
            viewModel = TwoFactorAuthenticationViewModelPreview.preview(),
            networkViewModels = listOf(
                WalletSettingsViewModelPreview.previewTwoFactor(),
                WalletSettingsViewModelPreview.previewTwoFactor()
            ),
            network = previewNetwork()
        )
    }
}

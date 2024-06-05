package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.common.models.settings.WalletSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun TwoFactorAuthenticationScreenPreview(
) {
    GreenAndroidPreview {
        TwoFactorAuthenticationScreen(
            viewModel = TwoFactorAuthenticationViewModel(previewWallet()),
            networkViewModels = listOf(WalletSettingsViewModelPreview.preview()),
            network = previewNetwork()
        )
    }
}

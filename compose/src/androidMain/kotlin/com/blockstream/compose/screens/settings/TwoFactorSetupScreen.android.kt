package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.common.models.settings.TwoFactorSetupViewModel
import com.blockstream.common.models.settings.TwoFactorSetupViewModelPreview
import com.blockstream.common.models.settings.WalletSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun TwoFactorSetupScreenScreenPreview(
) {
    GreenAndroidPreview {
        TwoFactorSetupScreen(
            viewModel = TwoFactorSetupViewModelPreview.preview(),
        )
    }
}

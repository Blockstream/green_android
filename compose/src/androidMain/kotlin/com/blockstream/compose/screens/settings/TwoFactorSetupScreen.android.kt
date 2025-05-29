package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.settings.TwoFactorSetupViewModelPreview
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

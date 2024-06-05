package com.blockstream.compose.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.onboarding.SetupNewWalletViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun SetupNewWalletScreenPreview() {
    GreenAndroidPreview {
        SetupNewWalletScreen(viewModel = SetupNewWalletViewModelPreview.preview())
    }
}
package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun WatchOnlyCredentialSinglesigPreview() {
    GreenAndroidPreview {
        WatchOnlyCredentialsScreen(
            viewModel = WatchOnlyCredentialsViewModelPreview.preview(
                isSinglesig = true
            )
        )
    }
}

@Composable
@Preview
fun WatchOnlyCredentialsSinglesigLiquidPreview() {
    GreenAndroidPreview {
        WatchOnlyCredentialsScreen(
            viewModel = WatchOnlyCredentialsViewModelPreview.preview(
                isSinglesig = true,
                isLiquid = true
            )
        )
    }
}
package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyNetworkViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun WatchOnlyNetworkScreenPreview() {
    GreenAndroidPreview {
        WatchOnlyNetworkScreen(viewModel = WatchOnlyNetworkViewModelPreview.preview())
    }
}
package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyPolicyViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun WatchOnlyPolicyScreenPreview() {
    GreenAndroidPreview {
        WatchOnlyPolicyScreen(viewModel = WatchOnlyPolicyViewModelPreview.preview())
    }
}
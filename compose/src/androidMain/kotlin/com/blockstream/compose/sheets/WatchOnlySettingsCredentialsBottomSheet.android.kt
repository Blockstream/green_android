package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.settings.WatchOnlyCredentialsSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun WatchOnlyCredentialsSettingsBottomSheetPreview() {
    GreenAndroidPreview {
        WatchOnlyCredentialsSettingsBottomSheet(
            viewModel = WatchOnlyCredentialsSettingsViewModelPreview.preview(),
            onDismissRequest = {}
        )
    }
}
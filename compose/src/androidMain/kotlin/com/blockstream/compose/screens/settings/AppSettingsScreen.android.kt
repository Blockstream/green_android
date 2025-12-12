package com.blockstream.compose.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.settings.AppSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AppSettingsScreenPreview() {
    GreenAndroidPreview {
        AppSettingsScreen(
            viewModel = AppSettingsViewModelPreview.preview(
                true
            )
        )
    }
}

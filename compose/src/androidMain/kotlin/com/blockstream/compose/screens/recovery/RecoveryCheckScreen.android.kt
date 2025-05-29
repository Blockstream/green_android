package com.blockstream.compose.screens.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.recovery.RecoveryCheckViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun RecoveryCheckScreenPreview() {
    GreenAndroidPreview {
        RecoveryCheckScreen(viewModel = RecoveryCheckViewModelPreview.preview())
    }
}
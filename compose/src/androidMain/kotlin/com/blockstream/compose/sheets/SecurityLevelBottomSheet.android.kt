package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.onboarding.SetupNewWalletViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SecurityLevelBottomSheetPreview() {
    GreenAndroidPreview {
        SecurityLevelBottomSheet(
            viewModel = SetupNewWalletViewModelPreview(),
            onDismissRequest = { }
        )
    }
}
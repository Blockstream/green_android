package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AskJadeUnlockOnboardingBottomSheetPreview() {
    GreenAndroidPreview {
        AskJadeUnlockBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            isOnboarding = true,
            onDismissRequest = { }
        )
    }
}

@Composable
@Preview
fun AskJadeUnlockBottomSheetPreview() {
    GreenAndroidPreview {
        AskJadeUnlockBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            isOnboarding = false,
            onDismissRequest = { }
        )
    }
}
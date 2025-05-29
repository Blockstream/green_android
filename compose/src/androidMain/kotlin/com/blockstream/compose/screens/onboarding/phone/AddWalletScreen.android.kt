package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.blockstream.common.models.onboarding.phone.AddWalletViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@PreviewScreenSizes
@Preview
fun AddWalletScreenPreview() {
    GreenAndroidPreview {
        AddWalletScreen(viewModel = AddWalletViewModelPreview.preview())
    }
}
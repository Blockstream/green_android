package com.blockstream.compose.screens.twofactor

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.twofactor.ReEnable2FAViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun ReEnable2FAScreenPreview() {
    GreenAndroidPreview {
        ReEnable2FAScreen(viewModel = ReEnable2FAViewModelPreview.preview())
    }
}
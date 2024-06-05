package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.send.SweepViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SweepScreenPreview() {
    GreenAndroidPreview {
        SweepScreen(viewModel = SweepViewModelPreview.preview())
    }
}

package com.blockstream.compose.screens.lightning

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.lightning.LnUrlAuthViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun LnUrlAuthScreenPreview() {
    GreenAndroidPreview {
        LnUrlAuthScreen(viewModel = LnUrlAuthViewModelPreview.preview())
    }
}

package com.blockstream.compose.screens.lightning

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.lightning.LnUrlAuthViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun LnUrlAuthScreenPreview() {
    GreenAndroidPreview {
        LnUrlAuthScreen(viewModel = LnUrlAuthViewModelPreview.preview())
    }
}

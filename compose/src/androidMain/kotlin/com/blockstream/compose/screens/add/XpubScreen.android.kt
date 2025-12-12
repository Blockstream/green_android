package com.blockstream.compose.screens.add

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.add.XpubViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun XpubScreenPreview() {
    GreenAndroidPreview {
        XpubScreen(viewModel = XpubViewModelPreview.preview())
    }
}

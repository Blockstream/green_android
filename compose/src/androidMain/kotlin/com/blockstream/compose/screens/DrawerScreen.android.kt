package com.blockstream.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun DrawerScreenPreview() {
    GreenAndroidPreview {
        DrawerScreen(viewModel = WalletsViewModelPreview.previewSoftwareOnly())
    }
}
package com.blockstream.compose.screens.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.devices.ImportPubKeyViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun ImportPubKeyScreenPreview() {
    GreenAndroidPreview {
        ImportPubKeyScreen(ImportPubKeyViewModelPreview())
    }
}
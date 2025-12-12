package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockstream.compose.models.abstract.AbstractScannerViewModel

@Composable
actual fun CameraView(
    modifier: Modifier,
    isFlashOn: Boolean,
    isDecodeContinuous: Boolean,
    showScanFromImage: Boolean,
    viewModel: AbstractScannerViewModel
) {
    Text("Not yet implemented")
}
package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockstream.compose.models.abstract.AbstractScannerViewModel

@Composable
expect fun CameraView(
    modifier: Modifier = Modifier,
    isFlashOn: Boolean = false,
    isDecodeContinuous: Boolean = true,
    showScanFromImage: Boolean = true,
    viewModel: AbstractScannerViewModel
)
package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.camera.CameraViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.ui.components.GreenColumn


@Composable
@Preview
fun CameraSurfacePreview() {
    GreenAndroidPreview {
        GreenColumn {
            GreenScanner(
                modifier = Modifier,
                isDecodeContinuous = true,
                showScanFromImage = true,
                viewModel = CameraViewModelPreview()
            )
        }
    }
}
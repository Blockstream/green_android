package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.camera.CameraViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.ui.components.GreenColumn

@Composable
@Preview
fun CameraBottomSheetPreview() {
    GreenAndroidPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            if (showBottomSheet) {
                CameraBottomSheet(
                    viewModel = CameraViewModelPreview.preview(),
                    onDismissRequest = {

                    }
                )
            }
        }
    }
}
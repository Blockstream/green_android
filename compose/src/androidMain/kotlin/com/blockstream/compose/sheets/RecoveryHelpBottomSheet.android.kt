package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.sheets.RecoveryHelpViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenColumn

@Composable
@Preview
fun RecoveryHelpBottomSheetPreview() {
    GreenAndroidPreview {
        GreenColumn {
            RecoveryHelpBottomSheet(viewModel = RecoveryHelpViewModelPreview.preview(), {

            })
        }
    }
}
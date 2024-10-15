package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.theme.GreenChromePreview


@Composable
@Preview
fun LightningShortcutDialogPreview() {
    GreenChromePreview {
        LightningShortcutDialog(
            viewModel = SimpleGreenViewModelPreview()
        ) {

        }
    }
}
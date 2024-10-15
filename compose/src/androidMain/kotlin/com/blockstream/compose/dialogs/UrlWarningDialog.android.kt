package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenPreview


@Composable
@Preview
fun UrlWarningDialogPreview() {
    GreenPreview {
        UrlWarningDialog(
            viewModel = SimpleGreenViewModelPreview(),
            urls = listOf("http://blockstream.com", "http://blockstream.io"),
            onDismiss = { _, _ ->

            }
        )
    }
}
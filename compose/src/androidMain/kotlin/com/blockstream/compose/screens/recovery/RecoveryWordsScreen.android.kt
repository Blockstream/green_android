package com.blockstream.compose.screens.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.recovery.RecoveryWordsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
private fun ItemPreview() {
    GreenThemePreview {
        GreenColumn {
            Item(1, "word")
        }
    }
}

@Composable
@Preview
fun RecoveryWordsScreenPreview() {
    GreenAndroidPreview {
        RecoveryWordsScreen(viewModel = RecoveryWordsViewModelPreview.preview())
    }
}
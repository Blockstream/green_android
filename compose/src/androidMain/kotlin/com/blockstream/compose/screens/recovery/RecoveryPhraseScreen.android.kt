package com.blockstream.compose.screens.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.recovery.RecoveryPhraseViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.components.GreenColumn

@Composable
@Preview
fun WordItemPreview() {
    GreenChromePreview {
        GreenColumn {
            WordItem(1, "chalk")
            WordItem(2, "patch")
            WordItem(3, "eye")
            WordItem(4, "speak")
        }
    }
}

@Composable
@Preview
fun RecoveryPhraseScreenPreview() {
    GreenAndroidPreview {
        RecoveryPhraseScreen(viewModel = RecoveryPhraseViewModelPreview.previewBip39())
    }
}
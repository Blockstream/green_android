package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.onboarding.phone.EnterRecoveryPhraseViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow

@Preview
@Composable
fun KeysPreview() {
    GreenChromePreview {
        GreenRow {
            Key(key = "about", isWord = true) { }
            Key(key = "thanks", isWord = true) { }
            Key(key = "rib", isWord = true) { }
        }
    }
}

@Preview
@Composable
fun PhraseW1ordPreview() {
    GreenChromePreview {
        GreenColumn {
            PhraseWord(1, "ribbon", true)
            PhraseWord(2, "ribbon", false)
            PhraseWord(3, "about", false)
        }
    }
}

@Composable
@Preview
@Preview(widthDp = 300, heightDp = 500)
fun EnterRecoveryPhrasePreview(
) {
    GreenAndroidPreview {
        EnterRecoveryPhraseScreen(viewModel = EnterRecoveryPhraseViewModelPreview.preview().also {
            it.recoveryPhrase.value = listOf("about", "thanks", "rib")
//            it.onProgress.value = true
        })
    }
}
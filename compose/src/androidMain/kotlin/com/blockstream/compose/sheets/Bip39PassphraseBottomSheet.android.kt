package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.login.Bip39PassphraseViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun Bip39PassphraseSheetPreview() {
    GreenAndroidPreview {
        Bip39PassphraseBottomSheet(
            viewModel = Bip39PassphraseViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}
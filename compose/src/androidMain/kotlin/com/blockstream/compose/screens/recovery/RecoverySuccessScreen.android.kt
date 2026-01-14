package com.blockstream.compose.screens.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewWallet
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun RecoverySuccessScreenPreview() {
    GreenAndroidPreview {
        RecoverySuccessScreen(greenWallet = previewWallet(), onDone = {})
    }
}


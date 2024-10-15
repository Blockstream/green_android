package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.compose.theme.GreenChromePreview

@Composable
@Preview
fun TwoFactorCodeDialoggPreview() {
    GreenChromePreview {
        TwoFactorCodeDialog(
            AuthHandlerStatus(action = "tes", method = "sms", status = "", attemptsRemaining = 2)
        ) { _, _ ->

        }
    }
}
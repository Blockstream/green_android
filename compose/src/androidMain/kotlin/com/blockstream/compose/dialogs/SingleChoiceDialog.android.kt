package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun SingleChoiceDialogPreview() {
    GreenAndroidPreview {
        SingleChoiceDialog(
            title = "Auto logout Timeout",
            items = listOf("1 Minutes", "2 Minute", "5 Minutes", "30 Minutes"),
            checkedItem = 0
        ) {

        }

        SingleChoiceDialog(
            title = "Security Change",
            message = "Another 2FA method is already active. Confirm via 2FA that you authorize this change.",
            items = listOf("Call", "SMS"),
            onNeutralText = "I lost my 2FA",
            onNeutralClick = {

            }
        ) {

        }
    }
}
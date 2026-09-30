package com.blockstream.compose.dialogs

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.components.AmountField
import com.blockstream.compose.components.AmountFieldState
import com.blockstream.compose.theme.GreenChromePreview

@Composable
@Preview
fun TextDialogPreview() {
    GreenChromePreview {
        AmountField(
            state = AmountFieldState(
                value = "",
                denominationLabel = "BTC",
            ),
            onValueChange = {},
            onAction = {},
        )

        TextDialog(
            title = "Title",
            message = "Message",
            label = "Label",
            supportingText = "Supporting Text",
            suffixText = "BTC",
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            )
        ) {

        }
    }
}

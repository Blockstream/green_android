package com.blockstream.compose.dialogs

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.Denomination
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.theme.GreenThemePreview

@Composable
@Preview
fun TextDialogPreview() {
    GreenThemePreview {
        GreenAmountField(value = "", onValueChange = {

        }, denomination = Denomination.BTC)

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
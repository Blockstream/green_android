package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.Denomination
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteLow


@Preview
@Composable
fun GreenAmountFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            var amount by remember {
                mutableStateOf("123")
            }
            GreenAmountField(amount, {
                amount = it
            }, footerContent = {
                Text(
                    text = "~ 1.131.00 EUR",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = bodyMedium,
                    color = whiteLow
                )
            }, denomination = Denomination.BTC)

            GreenAmountField(amount, {
                amount = it
            }, footerContent = {
                Text(
                    text = "~ 1.131.00 EUR",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = bodyMedium,
                    color = whiteLow
                )
            }, helperText = "id_invalid_amount", denomination = Denomination.SATOSHI)

            GreenAmountField(amount, {
                amount = it
            }, isAmountLocked = true, denomination = Denomination.MBTC)

            var isSendAll by remember {
                mutableStateOf(false)
            }
            GreenAmountField(amount, {
                amount = it
            }, sendAll = isSendAll, supportsSendAll = true, onSendAllClick = {
                isSendAll = !isSendAll
            }, denomination = Denomination.MBTC)

            GreenAmountField(amount, {
                amount = it
            }, sendAll = isSendAll, isReadyOnly = true, supportsSendAll = true, onSendAllClick = {
                isSendAll = !isSendAll
            }, denomination = Denomination.MBTC)
        }
    }
}
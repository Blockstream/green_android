package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.Denomination
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.ui.components.GreenColumn


@Preview
@Composable
fun GreenAmountFieldPreview() {
    GreenAndroidPreview {

        GreenColumn {
            var amount by remember {
                mutableStateOf("")
            }
            val secondaryValue by remember { derivedStateOf {
                amount
            } }
            GreenAmountField(amount, {
                amount = it
            }, secondaryValue = secondaryValue, denomination = Denomination.BTC)

            GreenAmountField(
                amount,
                {
                    amount = it
                },
                secondaryValue = "~ 1.131.00 EUR",
                helperText = "id_invalid_amount",
                denomination = Denomination.SATOSHI
            )

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

            GreenAmountField(
                amount,
                {
                    amount = it
                },
                secondaryValue = "",
                sendAll = isSendAll,
                isReadyOnly = true,
                supportsSendAll = true,
                onSendAllClick = {
                    isSendAll = !isSendAll
                },
                denomination = Denomination.MBTC
            )
        }
    }
}
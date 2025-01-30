package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.FeePriority
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.GreenChromePreview

@Preview
@Composable
fun GreenNetworkFeePreview() {
    GreenChromePreview {
        GreenColumn {
            GreenNetworkFee(FeePriority.High(), onClick = {})
            GreenNetworkFee(
                FeePriority.Medium(
                    fee = "1 BTC",
                    feeFiat = "12,00 USD",
                    feeRate = "1 sats/vbyte"
                ), onClick = {})
            GreenNetworkFee(FeePriority.Low(error = "id_insufficient_funds"), onClick = {})
            GreenNetworkFee(
                FeePriority.High(
                    fee = "1 BTC",
                    feeFiat = "12,00 USD",
                    feeRate = "1 sats/vbyte"
                ), withEditIcon = false, onClick = {})
        }
    }
}
package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.compose.theme.GreenChromePreview

@Preview
@Composable
fun GreenAmountPreview() {
    GreenChromePreview {
        GreenColumn {
            GreenAmount(
                title = "Amount",
                amount = "1.0 BTC",
                showIcon = true
            )
            GreenAmount(
                title = "Amount",
                amount = "1.0 BTC",
                amountFiat = "100.000 USD",
            )
            GreenAmount(
                title = "Amount",
                amount = "1.0 BTC",
                amountFiat = "100.000 USD",
                assetId = BTC_POLICY_ASSET,
                address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu"
            )
            GreenAmount(
                title = "Send To",
                amount = "1.0 BTC",
                amountFiat = "100.000 USD",
                assetId = BTC_POLICY_ASSET,
                showIcon = true,
                address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu"
            )
        }
    }
}
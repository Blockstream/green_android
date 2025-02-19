package com.blockstream.compose.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.looks.AmountAssetLook
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.GreenChromePreview


@Preview
@Composable
fun GreenAmountsPreview() {
    GreenChromePreview {
        GreenColumn {
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1.91080032",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "-1,500",
                        assetId = BTC_POLICY_ASSET,
                        "BTC"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "-0.768920",
                        assetId = BTC_POLICY_ASSET,
                        "BTC"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "2,420.1234",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1,232,321,543,322,420.1234567", assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    )
                )
            )

            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1.91080032",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    ),
                    AmountAssetLook("-2.1234", assetId = BTC_POLICY_ASSET, "LBTC", "5,1231.23 EUR")
                )
            )

            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1,232,321,543,322,420.1234567",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    ),
                    AmountAssetLook(
                        "-2,123,363,543,322,420.1234567",
                        assetId = BTC_POLICY_ASSET,
                        "LBTC"
                    )
                )
            )
            HorizontalDivider()
        }
    }
}
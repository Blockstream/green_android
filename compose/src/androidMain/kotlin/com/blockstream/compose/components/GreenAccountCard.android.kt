package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.LBTC_POLICY_ASSET
import com.blockstream.common.extensions.previewAccountBalance
import com.blockstream.compose.theme.GreenChromePreview

@Composable
@Preview
fun AccountAssetsPreview() {
    GreenChromePreview {
        AccountAssets(accountBalance = previewAccountBalance().let {
            it.copy(
                assets = listOf(
                    BTC_POLICY_ASSET,
                    BTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET
                )
            )
        })
    }
}

@Composable
@Preview
fun GreenAccountCardPreview() {
    GreenChromePreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, space = 1, padding = 0) {
            var expanded by remember {
                mutableIntStateOf(0)
            }
            GreenAccountCard(
                account = previewAccountBalance().let {
                    it.copy(
                        balance = null,
                        assets = listOf(
                            BTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET
                        )
                    )
                },
                isExpanded = expanded == 0,
                onClick = {
                    expanded = 0
                },
                onArrowClick = {

                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 0,
                onClick = {
                    expanded = 0
                },
                onCopyClick = {

                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 2,
                onClick = {
                    expanded = 2
                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 3,
                onClick = {
                    expanded = 3
                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 4,
                onWarningClick = {

                },
                onClick = {
                    expanded = 4
                })
        }
    }
}
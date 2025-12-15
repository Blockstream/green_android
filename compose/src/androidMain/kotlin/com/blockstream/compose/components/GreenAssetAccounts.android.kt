package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.previewAccount
import com.blockstream.compose.extensions.previewEnrichedAsset
import com.blockstream.compose.theme.GreenChromePreview

@Preview
@Composable
fun GreenAssetAccountsPreview() {
    GreenChromePreview {
        GreenColumn {
            var selected by remember {
                mutableStateOf(0)
            }
            GreenAssetAccounts(
                accounts = listOf(previewAccount(), previewAccount()),
                asset = previewEnrichedAsset(),
                isExpanded = selected == 0,
                onExpandClick = {
                    selected = 0
                }
            )
            GreenAssetAccounts(
                accounts = listOf(previewAccount(), previewAccount()),
                asset = previewEnrichedAsset(isLiquid = true),
                isExpanded = selected == 1,
                onExpandClick = {
                    selected = 1
                }
            )
            GreenAssetAccounts(
                accounts = listOf(previewAccount()),
                asset = previewEnrichedAsset(isLiquid = true),
                isExpanded = selected == 2,
                onExpandClick = {
                    selected = 2
                }
            )
            GreenAssetAccounts(
                asset = previewEnrichedAsset(true).copy(
                    name = "123456789098765432112345678987654321234567890",
                ),
                isExpanded = selected == 3,
                onExpandClick = {
                    selected = 3
                }
            )
            GreenAssetAccounts(
                asset = previewEnrichedAsset(true).copy(isAnyAsset = true),
                isExpanded = selected == 5,
                onExpandClick = {
                    selected = 5
                }
            )
        }
    }
}
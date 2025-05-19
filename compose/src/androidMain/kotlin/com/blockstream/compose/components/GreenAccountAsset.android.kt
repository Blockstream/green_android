package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account
import blockstream_green.common.generated.resources.id_account__asset
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.compose.GreenPreview
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource


@Preview
@Composable
fun GreenAccountAssetPreview() {
    GreenPreview {
        GreenColumn {
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                title = stringResource(Res.string.id_account__asset),
                withAssetIcon = false,
            )
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                title = stringResource(Res.string.id_account),
                withAsset = false,
                withAssetIcon = false,
            )
            GreenAccountAsset(accountAssetBalance = previewAccountAsset().let {
                AccountAssetBalance(
                    account = it.account,
                    asset = it.asset,
                    balance = "123 BTC",
                    balanceExchange = "45,000 USD"
                )
            })
            GreenAccountAsset(accountAssetBalance = previewAccountAsset().let {
                AccountAssetBalance(
                    account = it.account,
                    asset = it.asset,
                    balance = "123 BTC",
                    balanceExchange = "45,000 USD"
                )
            }, withAsset = false)
            GreenAccountAsset(accountAssetBalance = previewAccountAsset().let {
                AccountAssetBalance(
                    account = it.account,
                    asset = it.asset,
                    balance = "123 BTC",
                    balanceExchange = "45,000 USD"
                )
            }, withEditIcon = true)

            GreenAccountAsset(accountAssetBalance = previewAccountAsset().let {
                AccountAssetBalance(
                    account = it.account.copy(
                        gdkName = "Account Name Quite Large"
                    ),
                    asset = it.asset.copy(
                        name = "Bitcoin with Quite Large Name"
                    ),
                    balance = "1,123,1231 BTC",
                    balanceExchange = "23,432,425,445 USD"
                )
            }, withEditIcon = true)
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                withEditIcon = true,
                onClick = {})
            GreenAccountAsset(withEditIcon = true, onClick = {})
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                message = "Redeposit Expired 2FA coins",
                withAsset = false,
                withArrow = true,
                onClick = {})
        }
    }
}
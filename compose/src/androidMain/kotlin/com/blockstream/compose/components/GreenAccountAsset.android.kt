package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.pencil_simple_line
import blockstream_green.common.generated.resources.unknown
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource


@Preview
@Composable
fun GreenAccountAssetPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                title = stringResource(id = R.string.id_account__asset)
            )
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                title = stringResource(id = R.string.id_account),
                withAsset = false
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
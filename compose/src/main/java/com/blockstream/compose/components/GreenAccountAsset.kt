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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.whiteMedium

@Composable
fun GreenAccountAsset(
    accountAssetBalance: AccountAssetBalance? = null,
    session: GdkSession? = null,
    title: String? = null,
    withEditIcon: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    GreenDataLayout(title = title, onClick = onClick, withPadding = false) {

        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = accountAssetBalance?.let {
                        it.asset.assetId.assetIcon(session = session, isLightning = accountAssetBalance.account.isLightning)
                    } ?: painterResource(
                        id = R.drawable.unknown
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(end = 7.dp)
                        .size(32.dp)
                )

                if (accountAssetBalance != null) {
                    Image(
                        painter = painterResource(id = accountAssetBalance.account.policyIcon()),
                        contentDescription = "Policy",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 7.dp)
                            .size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .padding(end = if(withEditIcon && onClick != null) 0.dp else 10.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (accountAssetBalance == null) {
                    Text(
                        text = stringResource(id = R.string.id_select_asset),
                        style = labelLarge,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Asset
                            Text(
                                text = accountAssetBalance.asset.name(session),
                                style = labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Account Name
                            Text(
                                text = accountAssetBalance.account.name,
                                style = labelMedium,
                                color = whiteMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Account Policy
                            Text(
                                text = accountAssetBalance.account.type.toString().uppercase(),
                                style = labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (accountAssetBalance.balance != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                // Amount
                                Text(
                                    text = accountAssetBalance.balance ?: "",
                                    style = labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = whiteMedium
                                )

                                accountAssetBalance.balanceExchange?.also {
                                    // Fiat
                                    Text(
                                        text = it,
                                        style = bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = whiteMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (withEditIcon && onClick != null) {
                IconButton(onClick = onClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.pencil_simple_line),
                        contentDescription = "Edit",
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun GreenAccountAssetPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAccountAsset(
                accountAssetBalance = previewAccountAsset().accountAssetBalance,
                title = stringResource(id = R.string.id_account__asset)
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
            GreenAccountAsset(accountAssetBalance = previewAccountAsset().accountAssetBalance, withEditIcon = true)
            GreenAccountAsset(withEditIcon = true)
        }
    }
}
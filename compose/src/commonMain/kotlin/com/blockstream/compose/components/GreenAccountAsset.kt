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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_select_account
import blockstream_green.common.generated.resources.id_select_account__asset
import blockstream_green.common.generated.resources.pencil_simple_line
import blockstream_green.common.generated.resources.unknown
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAccountAsset(
    modifier: Modifier = Modifier,
    accountAssetBalance: AccountAssetBalance? = null,
    session: GdkSession? = null,
    title: String? = null,
    message: String? = null,
    selectText: String? = null,
    withAsset: Boolean = true,
    withEditIcon: Boolean = false,
    withArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    GreenDataLayout(modifier = modifier, title = title, onClick = onClick, withPadding = false) {

        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = accountAssetBalance?.let {
                        it.asset.assetId.assetIcon(session = session, isLightning = accountAssetBalance.account.isLightning)
                    } ?: painterResource(
                        Res.drawable.unknown
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
                        painter = painterResource(accountAssetBalance.account.policyIcon()),
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
                    .padding(end = if (withEditIcon && onClick != null) 0.dp else 10.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (accountAssetBalance == null) {
                    Text(
                        text = selectText ?: stringResource(if(withAsset) Res.string.id_select_account__asset else Res.string.id_select_account),
                        style = labelLarge,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier
                            .weight(1f)
                            .ifTrue(message != null) {
                                padding(vertical = 16.dp)
                            }) {
                            val primary = if(withAsset) accountAssetBalance.asset.name(session) else StringHolder.create(accountAssetBalance.account.name)
                            val secondary = if(withAsset) accountAssetBalance.account.name else null

                            // Asset or Account
                            Text(
                                text = primary.string(),
                                style = titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if(secondary != null) {
                                // Account Name
                                Text(
                                    text = secondary,
                                    style = labelLarge,
                                    color = whiteMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Account Policy
                            Text(
                                text = accountAssetBalance.account.type.toString().uppercase(),
                                style = labelMedium.copy(fontSize = 8.sp, lineHeight = 12.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = whiteLow
                            )

                            message?.also {
                                Text(
                                    text = it,
                                    style = labelMedium,
                                    color = whiteMedium,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
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
                        painter = painterResource(Res.drawable.pencil_simple_line),
                        contentDescription = "Edit",
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            if(withArrow) {
                GreenArrow(modifier = Modifier.padding(end = 16.dp))
            }
        }
    }
}

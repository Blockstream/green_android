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
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_select_asset
import blockstream_green.common.generated.resources.pencil_simple_line
import blockstream_green.common.generated.resources.unknown
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.ConsistentHeightBox
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAsset(
    modifier: Modifier = Modifier,
    assetBalance: AssetBalance? = null,
    session: GdkSession? = null,
    title: String? = null,
    selectText: String? = null,
    withEditIcon: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    GreenDataLayout(modifier = modifier, title = title, onClick = onClick, withPadding = false) {

        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = assetBalance?.asset?.assetId?.assetIcon(session = session)
                        ?: painterResource(Res.drawable.unknown),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(end = 8.dp)
                        .size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .padding(end = if (withEditIcon && onClick != null) 0.dp else 16.dp)
                    .padding(vertical = 16.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (assetBalance == null) {
                    Text(
                        text = selectText ?: stringResource(Res.string.id_select_asset),
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
                                text = assetBalance.asset.name(session).string(),
                                style = titleSmall,
                                color = whiteHigh,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (assetBalance.balance != null) {
                            ConsistentHeightBox(placeholder = {
                                BalanceDetails("0", "0")
                            }, content = {
                                BalanceDetails(assetBalance.balance, assetBalance.balanceExchange)
                            })
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
        }
    }
}

@Composable
private fun BalanceDetails(assetBalance: String?, fiatAssetBalance: String?) {
    Column(
        horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = assetBalance ?: "", style = labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = green
        )

        fiatAssetBalance?.also {
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
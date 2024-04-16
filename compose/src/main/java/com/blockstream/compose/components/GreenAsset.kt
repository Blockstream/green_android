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
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh

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
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = assetBalance?.let {
                        it.asset.assetId.assetIcon(session = session)
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

//                if (assetBalance != null) {
//                    Image(
//                        painter = painterResource(id = assetBalance.assetId.assetIcon(session)),
//                        contentDescription = "Policy",
//                        modifier = Modifier
//                            .align(Alignment.BottomEnd)
//                            .padding(bottom = 7.dp)
//                            .size(18.dp)
//                    )
//                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .padding(end = if (withEditIcon && onClick != null) 0.dp else 10.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (assetBalance == null) {
                    Text(
                        text = selectText ?: stringResource(id = R.string.id_select_asset),
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
                                text = assetBalance.asset.name(session),
                                style = titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (assetBalance.balance != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                // Amount
                                Text(
                                    text = assetBalance.balance ?: "",
                                    style = labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = whiteHigh
                                )

                                assetBalance.balanceExchange?.also {
                                    // Fiat
                                    Text(
                                        text = it,
                                        style = bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = whiteHigh,
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
fun GreenAssetPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAsset(
                assetBalance = previewAssetBalance(),
                title = stringResource(id = R.string.id_asset)
            )

            GreenAsset(assetBalance = previewAssetBalance(), withEditIcon = true)

            GreenAsset(withEditIcon = true, onClick = {})
        }
    }
}
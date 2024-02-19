package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Text
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
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium

@Composable
fun GreenAccountAsset(
    accountAsset: AccountAsset,
    withBalance: Boolean = false,
    session: GdkSession? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.id_account__asset),
            style = labelMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Card(modifier = Modifier.clickable {
            onClick()
        }) {
            Row(
                modifier = Modifier.padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Image(
                        painter = accountAsset.asset.assetId.assetIcon(session = session),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .padding(end = 7.dp)
                            .size(32.dp)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.key_multisig),
                        contentDescription = "Policy",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 7.dp)
                            .size(18.dp)
                    )

                }

                Column(modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)) {
                    // Asset
                    Text(
                        text = accountAsset.asset.name(session),
                        style = labelLarge,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Account Name
                    Text(
                        text = accountAsset.account.name.uppercase(),
                        style = labelMedium,
                        color = whiteMedium,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Account Policy
                    Text(
                        text = "Segwit".uppercase(),
                        style = labelSmall,
                        color = whiteLow,
                        overflow = TextOverflow.Ellipsis
                    )
                }

//                if(withBalance) {
//                    Column(
//                        modifier = Modifier
//                            .padding(start = 12.dp)
//                            .weight(1f)
//                            .padding(end = 16.dp),
//                        horizontalAlignment = Alignment.End
//                    ) {
//                        // Amount
//                        Text(
//                            text = "123, BTC",
//                            style = labelMedium,
//                            overflow = TextOverflow.Ellipsis
//                        )
//
//                        // Fiat
//                        Text(
//                            text = "1,200.00 USD",
//                            style = labelSmall,
//                            color = whiteMedium,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                }

                Image(
                    painter = painterResource(id = R.drawable.pencil_simple_line),
                    contentDescription = "Edit",
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun GreenAccountAssetPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAccountAsset(accountAsset = previewAccountAsset())
            GreenAccountAsset(accountAsset = previewAccountAsset())
            GreenAccountAsset(accountAsset = previewAccountAsset())
        }
    }
}
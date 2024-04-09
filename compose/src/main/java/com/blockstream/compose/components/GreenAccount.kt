package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteMedium

@Composable
fun GreenAccount(
    account: Account?,
    session: GdkSession? = null,
    title: String? = null,
    withEditIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    GreenDataLayout(title = title, onClick = onClick, withPadding = false) {

        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = account?.let {
                        it.network.policyAsset.assetIcon(
                            session = session,
                            isLightning = it.isLightning
                        )
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

                if (account != null) {
                    Image(
                        painter = painterResource(id = account.policyIcon()),
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
                    .padding(end = if (withEditIcon && onClick != null) 0.dp else 16.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (account == null) {
                    Text(
                        text = stringResource(id = R.string.id_select_account),
                        style = labelLarge,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Column {
                        // Account Name
                        Text(
                            text = account.name,
                            style = labelLarge,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Account
                        Text(
                            text = account.type.toString(),
                            style = labelMedium,
                            color = whiteMedium,
                            overflow = TextOverflow.Ellipsis
                        )
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
fun GreenAccountPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAccount(account = previewAccount(), withEditIcon = true)
            GreenAccount(account = previewAccount())
            GreenAccount(account = previewAccount())
            GreenAccount(account = null, withEditIcon = true, onClick = {})
        }
    }
}
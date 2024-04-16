package com.blockstream.compose.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.looks.account.LightningInfoLook
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.md_theme_brandSurface
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.stringResourceId

@Composable
fun LightningInfo(
    lightningInfoLook: LightningInfoLook,
    onSweepClick: () -> Unit = {},
    onLearnMore: () -> Unit = {}
) {
    GreenColumn(
        padding = 0,
        space = 8,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {

        lightningInfoLook.sweep?.also {
            GreenCard(
                colors = CardDefaults.elevatedCardColors(containerColor = md_theme_brandSurface),
                padding = 0,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        )
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(text = stringResourceId(it), color = whiteMedium, style = labelMedium)
                    GreenButton(
                        text = stringResource(id = R.string.id_sweep),
                        type = GreenButtonType.TEXT,
                        size = GreenButtonSize.SMALL,
                        modifier = Modifier.align(Alignment.End),
                        onClick = onSweepClick
                    )
                }
            }
        }
        lightningInfoLook.capacity?.also {
            GreenCard(
                colors = CardDefaults.elevatedCardColors(containerColor = md_theme_brandSurface),
                padding = 0,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        )
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    GreenRow(padding = 0, space = 8) {
                        Icon(
                            painter = painterResource(id = R.drawable.info),
                            contentDescription = null,
                            tint = whiteMedium
                        )
                        Text(text = stringResourceId(it), color = whiteMedium, style = labelMedium)
                    }
                    GreenButton(
                        text = stringResource(id = R.string.id_learn_more),
                        type = GreenButtonType.TEXT,
                        size = GreenButtonSize.SMALL,
                        modifier = Modifier.padding(start = 24.dp),
                        onClick = onLearnMore
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LightningInfoPreview() {
    GreenTheme {
        GreenColumn(padding = 0, space = 4) {
            LightningInfo(
                lightningInfoLook = LightningInfoLook(
                    sweep = "You can sweep 1 BTC of your funds to your onchain account.",
                    capacity = "Your current receive capacity is 1 BTC."
                )
            )
        }
    }
}
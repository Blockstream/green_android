package com.blockstream.compose.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_sweep
import blockstream_green.common.generated.resources.info
import com.blockstream.common.looks.account.LightningInfoLook
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.md_theme_brandSurface
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
                    Text(text = it, color = whiteMedium, style = labelMedium)
                    GreenButton(
                        text = stringResource(Res.string.id_sweep),
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
                            painter = painterResource(Res.drawable.info),
                            contentDescription = null,
                            tint = whiteMedium
                        )
                        Text(text = it, color = whiteMedium, style = labelMedium)
                    }
                    GreenButton(
                        text = stringResource(Res.string.id_learn_more),
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
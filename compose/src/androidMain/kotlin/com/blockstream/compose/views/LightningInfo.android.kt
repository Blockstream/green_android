package com.blockstream.compose.views

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.looks.account.LightningInfoLook
import com.blockstream.compose.GreenPreview
import com.blockstream.ui.components.GreenColumn


@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LightningInfoPreview() {
    GreenPreview {
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
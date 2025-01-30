@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.views

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.compose.GreenPreview
import com.blockstream.ui.components.GreenColumn

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WalletListItemPreview() {
    GreenPreview {
        GreenColumn(space = 4) {
            WalletListItem(WalletListLook.preview(hasLightningShortcut = true))

            WalletListItem(WalletListLook.preview(false, false))
            WalletListItem(WalletListLook.preview(false, true))

            WalletListItem(WalletListLook.preview(true, false))
            WalletListItem(WalletListLook.preview(true, true))
        }
    }
}
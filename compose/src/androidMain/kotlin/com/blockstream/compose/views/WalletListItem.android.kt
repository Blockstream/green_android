@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.views

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.caret_right
import blockstream_green.common.generated.resources.lightning_slash
import blockstream_green.common.generated.resources.text_aa
import blockstream_green.common.generated.resources.trash
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenCircle
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.extensions.resource
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WalletListItemPreview() {
    GreenTheme {
        GreenColumn(space = 4) {
            WalletListItem(WalletListLook.preview(hasLightningShortcut = true))

            WalletListItem(WalletListLook.preview(false, false))
            WalletListItem(WalletListLook.preview(false, true))

            WalletListItem(WalletListLook.preview(true, false))
            WalletListItem(WalletListLook.preview(true, true))
        }
    }
}
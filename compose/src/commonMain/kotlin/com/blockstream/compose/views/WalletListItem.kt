@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.pencil_simple
import blockstream_green.common.generated.resources.trash
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.SwipeAction
import com.blockstream.compose.components.SwipeableActionsContainer
import com.blockstream.compose.looks.wallet.WalletListLook
import com.blockstream.compose.theme.GreenColors
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.appTestTag
import com.blockstream.data.data.GreenWallet

@Composable
private fun WalletListRow(
    title: String,
    subtitle: String,
    isWatchOnly: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit = {},
) {
    GreenRow(
        padding = 0,
        space = 8,
        modifier = Modifier
            .appTestTag(title)
            .combinedClickable(onClick = {
                onClick.invoke()
            })
            .padding(horizontal = 12.dp)
            .height(70.dp)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, style = bodySmall, color = whiteLow)
        }
        Icon(
            imageVector = PhosphorIcons.Regular.CaretRight,
            contentDescription = null,
            tint = whiteMedium,
            modifier = Modifier.size(24.dp)
        )
    }
}

open class WalletListItemCallbacks(
    val onWalletClick: (wallet: GreenWallet) -> Unit,
    val onWalletDelete: ((wallet: GreenWallet) -> Unit),
    val onWalletRename: ((wallet: GreenWallet) -> Unit),
) {
    companion object {
        val Empty = WalletListItemCallbacks({}, {}, {})
    }
}

@Composable
fun WalletListItem(
    look: WalletListLook,
    callbacks: WalletListItemCallbacks = WalletListItemCallbacks.Empty,
    onSwipe: (String?) -> Unit,
    isSwiped: Boolean
) {
    val actions = listOf(
        SwipeAction(
            icon = Res.drawable.pencil_simple,
            backgroundColor = GreenColors.surface,
            testTag = "rename_wallet_${look.greenWallet.id}",
            contentDescription = "Rename wallet",
            onClick = { callbacks.onWalletRename(look.greenWallet) }
        ),
        SwipeAction(
            icon = Res.drawable.trash,
            backgroundColor = GreenColors.error,
            testTag = "remove_wallet_${look.greenWallet.id}",
            contentDescription = "Remove wallet",
            onClick = { callbacks.onWalletDelete(look.greenWallet) }
        )
    )

    SwipeableActionsContainer(
        isSwiped = isSwiped,
        onSwipe = { opened -> onSwipe(if (opened) look.greenWallet.id else null) },
        actions = actions
    ) {
        GreenCard(padding = 0, modifier = Modifier.fillMaxWidth()) {
            WalletListRow(
                title = look.title,
                subtitle = look.subtitle,
                isWatchOnly = look.isWatchOnly,
                isConnected = look.isConnected,
                onClick = { callbacks.onWalletClick(look.greenWallet) }
            )
        }
    }
}
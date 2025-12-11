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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_remove_wallet
import blockstream_green.common.generated.resources.id_rename_wallet
import blockstream_green.common.generated.resources.text_aa
import blockstream_green.common.generated.resources.trash
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.utils.appTestTag
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun WalletListRow(
    title: String,
    subtitle: String,
    isWatchOnly: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    GreenRow(
        padding = 0,
        space = 8,
        modifier = Modifier
            .appTestTag(title)
            .combinedClickable(onClick = {
                onClick.invoke()
            }, onLongClick = {
                onLongClick.invoke()
            })
            .padding(horizontal = 16.dp)
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
            Text(text = subtitle, style = bodySmall, color = whiteMedium)
        }

//        if (isWatchOnly) {
//            Icon(
//                imageVector = PhosphorIcons.Regular.Binoculars,
//                contentDescription = null,
//                tint = whiteLow
//            )
//        }

//        if (isConnected) {
//            GreenCircle(size = 6)
//        }

        Icon(
            imageVector = PhosphorIcons.Regular.CaretRight,
            contentDescription = null,
            tint = whiteLow,
            modifier = Modifier.size(16.dp)
        )
    }
}

open class WalletListItemCallbacks constructor(
    val onWalletClick: (wallet: GreenWallet) -> Unit,
    val onWalletDelete: ((wallet: GreenWallet) -> Unit),
    val onWalletRename: ((wallet: GreenWallet) -> Unit),
    val hasContextMenu: Boolean = false,
) {
    companion object {
        val Empty = WalletListItemCallbacks({}, {}, {}, false)
    }
}

@Composable
fun WalletListItem(
    look: WalletListLook,
    callbacks: WalletListItemCallbacks = WalletListItemCallbacks.Empty
) {
    val popupState = remember {
        PopupState()
    }

    val density = LocalDensity.current

    GreenCard(
        padding = 0,
        modifier = Modifier
            .combinedClickable(onClick = {

            }, onLongClick = {
                popupState.isContextMenuVisible.value = true
            })
            .onSizeChanged {
                popupState.offset.value =
                    with(density) { DpOffset(16.dp, (-it.height.toDp() + 16.dp + 24.dp)) }
            }
            .fillMaxWidth()
    ) {
        WalletListRow(
            title = look.title,
            subtitle = look.subtitle,
            isWatchOnly = look.isWatchOnly,
            isConnected = look.isConnected,
            onClick = {
                callbacks.onWalletClick.invoke(look.greenWallet)
            },
            onLongClick = {
                popupState.isContextMenuVisible.value = true
            }
        )

        if (callbacks.hasContextMenu) {
            PopupMenu(
                state = popupState,
                entries = listOf(
                    MenuEntry(
                        title = stringResource(Res.string.id_rename_wallet),
                        iconRes = Res.drawable.text_aa,
                        onClick = {
                            callbacks.onWalletRename.invoke(look.greenWallet)
                        }
                    ),
                    MenuEntry(
                        title = stringResource(Res.string.id_remove_wallet),
                        iconRes = Res.drawable.trash,
                        onClick = {
                            callbacks.onWalletDelete.invoke(look.greenWallet)
                        }
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun WalletListItemPreview() {
    GreenPreview {
        GreenColumn(space = 4) {
            WalletListItem(WalletListLook.preview(isConnected = true))

            WalletListItem(WalletListLook.preview(false, false))
            WalletListItem(WalletListLook.preview(false, true))

            WalletListItem(WalletListLook.preview(true, false))
            WalletListItem(WalletListLook.preview(true, true))
        }
    }
}
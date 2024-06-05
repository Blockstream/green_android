@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.views

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.caret_right
import blockstream_green.common.generated.resources.id_remove_lightning_shortcut
import blockstream_green.common.generated.resources.id_remove_wallet
import blockstream_green.common.generated.resources.id_rename_wallet
import blockstream_green.common.generated.resources.lightning_slash
import blockstream_green.common.generated.resources.text_aa
import blockstream_green.common.generated.resources.trash
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.compose.components.GreenCircle
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.extensions.resource
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun WalletListRow(
    title: String,
    subtitle: String? = null,
    walletIcon: WalletIcon,
    isConnected: Boolean,
    isLightning: Boolean,
    onClick: (isLightning: Boolean) -> Unit = {},
    onLongClick: (isLightning: Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .combinedClickable(onClick = {
                onClick.invoke(isLightning)
            }, onLongClick = {
                onLongClick.invoke(isLightning)
            })
            .padding(horizontal = 16.dp)
            .ifTrue(isLightning) {
                padding(start = 32.dp)
            }
            .height(52.dp)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(walletIcon.resource()),
            contentDescription = null,
        )

        GreenSpacer(space = 8)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let { Text(text = it, style = bodySmall) }
        }

        GreenSpacer()

        if (isConnected) {
            GreenCircle(size = 6)
        }

        GreenSpacer(space = 4)

        Icon(
            painter = painterResource(Res.drawable.caret_right),
            contentDescription = null,
            tint = whiteLow,
            modifier = Modifier.size(16.dp)
        )
    }
}
open class WalletListItemCallbacks constructor(
    val onWalletClick: (wallet: GreenWallet, isLightning: Boolean) -> Unit,
    val onLightningShortcutDelete: ((wallet: GreenWallet) -> Unit),
    val onWalletDelete: ((wallet: GreenWallet) -> Unit),
    val onWalletRename: ((wallet: GreenWallet) -> Unit),
    val hasContextMenu: Boolean = false,
){
    companion object{
        val Empty = WalletListItemCallbacks({_, _ ->}, {}, {} , {} , false)
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
    var isLightningPopup by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    Card(
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
            walletIcon = look.icon,
            isConnected = look.isConnected,
            isLightning = false,
            onClick = {
                 callbacks.onWalletClick.invoke(look.greenWallet, it)
            },
            onLongClick = {
                isLightningPopup = it
                popupState.isContextMenuVisible.value = true
            }
        )

        if (look.hasLightningShortcut) {
            HorizontalDivider()
            WalletListRow(
                title = "Lightning Account",
                walletIcon = WalletIcon.LIGHTNING,
                isConnected = look.isLightningShortcutConnected,
                isLightning = true,
                onClick = {
                    callbacks.onWalletClick.invoke(look.greenWallet, it)
                },
                onLongClick = {
                    isLightningPopup = it
                    popupState.isContextMenuVisible.value = true
                }
            )
        }

        if (callbacks.hasContextMenu) {
            PopupMenu(
                state = popupState,
                entries = if (isLightningPopup) {
                    listOf(
                        MenuEntry(
                            title = stringResource(Res.string.id_remove_lightning_shortcut),
                            iconRes = Res.drawable.lightning_slash,
                            onClick = {
                                callbacks.onLightningShortcutDelete.invoke(look.greenWallet)
                            })
                    )
                } else {
                    listOf(
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
                }
            )
        }


    }
}
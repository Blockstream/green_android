package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.question
import com.blockstream.compose.extensions.toggle
import com.blockstream.compose.theme.GreenTheme


@Composable
@Preview
fun PopupMenuPreview() {

    val popupState = remember { PopupState().also { it.isContextMenuVisible.value = true } }

    GreenTheme {
        Box {
            GreenColumn(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()) {
                GreenButton(text = "isContextMenuVisible: ${popupState.isContextMenuVisible.value}") {
                    popupState.isContextMenuVisible.toggle()
                }
                PopupMenu(state = popupState, entries = listOf(MenuEntry(title = "Menu 1"), MenuEntry(title = "Menu 2", iconRes = Res.drawable.question )))
            }
        }
    }
}
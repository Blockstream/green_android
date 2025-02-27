package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Question
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.extensions.toggle


@Composable
@Preview
fun PopupMenuPreview() {

    val popupState = remember { PopupState().also { it.isContextMenuVisible.value = true } }

    GreenPreview {
        Box {
            GreenColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                GreenButton(text = "isContextMenuVisible: ${popupState.isContextMenuVisible.value}") {
                    popupState.isContextMenuVisible.toggle()
                }
                PopupMenu(
                    state = popupState,
                    entries = listOf(
                        MenuEntry(title = "Menu 1"),
                        MenuEntry(title = "Menu 2", imageVector = PhosphorIcons.Regular.Question)
                    )
                )
            }
        }
    }
}
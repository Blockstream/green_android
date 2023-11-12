package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import com.blockstream.compose.R
import com.blockstream.compose.extensions.toggle
import com.blockstream.compose.theme.GreenTheme

data class MenuEntry(
    val title: String,
    val iconRes: Int? = null,
    val showAsAction: Boolean = false,
    val onClick: () -> Unit = {}
)

data class PopupState(
    val isContextMenuVisible: MutableState<Boolean> = mutableStateOf(false),
    val offset: MutableState<DpOffset> = mutableStateOf(DpOffset.Zero),
)


@Composable
fun PopupMenu(state: PopupState, entries: List<MenuEntry>) {
    DropdownMenu(
        expanded = state.isContextMenuVisible.value,
        onDismissRequest = {
            state.isContextMenuVisible.value = false
        },
        offset = state.offset.value
    ) {
        entries.forEach {
            DropdownMenuItem(
                text = { Text(it.title) },
                onClick = {
                    it.onClick.invoke()
                    state.isContextMenuVisible.value = false
                },
                leadingIcon = it.iconRes?.let {
                    {
                        Icon(
                            painterResource(id = it),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    }
}

@Composable
@Preview
fun PopupMenuPreview() {

    val popupState = remember { PopupState().also { it.isContextMenuVisible.value = true } }

    GreenTheme {
        Box {
            GreenColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                GreenButton(text = "isContextMenuVisible: ${popupState.isContextMenuVisible.value}") {
                    popupState.isContextMenuVisible.toggle()
                }
                PopupMenu(popupState, listOf(MenuEntry(title = "Menu 1"), MenuEntry(title = "Menu 2", iconRes = R.drawable.question )))
            }
        }
    }
}
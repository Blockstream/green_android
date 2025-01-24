package com.blockstream.compose.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import com.blockstream.common.data.NavAction
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class MenuEntry(
    val title: String,
    val iconRes: DrawableResource? = null,
    val imageVector: ImageVector? = null,
    val onClick: () -> Unit = {}
){
    companion object{

        @Composable
        fun from(navAction: NavAction): MenuEntry {
            return MenuEntry(
                title = navAction.title,
                iconRes = navAction.icon,
                onClick = navAction.onClick
            )
        }
    }
}

data class PopupState(
    val isContextMenuVisible: MutableState<Boolean> = mutableStateOf(false),
    val offset: MutableState<DpOffset> = mutableStateOf(DpOffset.Zero),
)


@Composable
fun PopupMenu(modifier: Modifier = Modifier, state: PopupState, entries: List<MenuEntry>) {
    DropdownMenu(
        modifier = modifier,
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
                            painterResource(it),
                            contentDescription = null
                        )
                    }
                } ?: it.imageVector?.let {
                    {
                        Icon(
                            imageVector = it,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    }
}

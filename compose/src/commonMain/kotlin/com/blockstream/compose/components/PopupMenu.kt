package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.DotsThreeVertical
import com.blockstream.compose.theme.labelMedium
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
                title = navAction.title ?: stringResource(navAction.titleRes!!),
                iconRes = navAction.icon,
                imageVector = navAction.imageVector,
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


@Composable
fun ActionMenu(
    navData: NavData,
){
    val popupState = remember { PopupState() }

    AnimatedVisibility(
        visible = navData.isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row {
            val actionsMenu by derivedStateOf {
                navData.actions.filter { !it.isMenuEntry }
            }

            actionsMenu.forEach {
                if (it.icon == null) {
                    TextButton(
                        onClick = it.onClick,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(text = it.title ?: stringResource(it.titleRes!!), style = labelMedium)
                    }
                } else {
                    IconButton(onClick = {
                        it.onClick()
                    }) {
                        it.icon?.also {
                            Icon(
                                painter = painterResource(it),
                                contentDescription = null
                            )
                        }
                        it.imageVector?.also {
                            Icon(
                                imageVector = it,
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            val contextMenu by derivedStateOf {
                navData.actions.filter { it.isMenuEntry }
            }

            if (contextMenu.isNotEmpty()) {
                IconButton(onClick = {
                    popupState.isContextMenuVisible.value = true
                }) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.DotsThreeVertical,
                        contentDescription = "More Menu"
                    )
                }

                PopupMenu(state = popupState, entries = contextMenu.map {
                    MenuEntry.from(it)
                })
            }
        }
    }
}
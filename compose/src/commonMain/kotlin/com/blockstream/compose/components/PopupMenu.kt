package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.DotsThreeVertical
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.IconContainer
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.utils.appTestTag
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class MenuEntry(
    val title: String,
    val iconRes: DrawableResource? = null,
    val imageVector: ImageVector? = null,
    val testTag: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
) {
    companion object {

        @Composable
        fun from(navAction: NavAction.Menu): MenuEntry {
            return MenuEntry(
                title = navAction.title ?: stringResource(navAction.titleRes!!),
                iconRes = navAction.icon,
                imageVector = navAction.imageVector,
                testTag = navAction.testTag,
                enabled = navAction.enabled,
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
                modifier = Modifier.appTestTag(it.testTag),
                text = { Text(it.title) },
                enabled = it.enabled,
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

private val NAV_ACTION_ICON_SIZE = 44.dp
private val NAV_ACTION_ICON_END_PADDING = 12.dp

@Composable
fun ActionMenu(
    navData: NavData,
) {
    val popupState = remember { PopupState() }

    AnimatedVisibility(
        visible = navData.isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            navData.actions.forEach { action ->
                when (action) {
                    is NavAction.Text -> {
                        val title =
                            action.title ?: action.titleRes?.let { titleRes -> stringResource(titleRes) }
                        TextButton(
                            onClick = action.onClick,
                            modifier = Modifier.appTestTag(
                                action.testTag ?: action.titleRes?.key ?: action.title
                            ),
                            enabled = action.enabled,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                NavActionIcon(
                                    icon = action.icon,
                                    imageVector = action.imageVector,
                                    iconTint = action.iconTint,
                                    iconSize = action.iconSize
                                )
                                title?.also {
                                    Text(text = it, style = labelMedium)
                                }
                            }
                        }
                    }

                    is NavAction.Icon -> {
                        Box(
                            modifier = Modifier
                                .appTestTag(action.testTag ?: action.titleRes?.key ?: action.title)
                                .padding(end = NAV_ACTION_ICON_END_PADDING)
                                .size(NAV_ACTION_ICON_SIZE)
                                .clip(CircleShape)
                                .then(
                                    when (action.container) {
                                        IconContainer.NONE -> Modifier
                                        IconContainer.OUTLINED -> Modifier
                                            .background(color = MaterialTheme.colorScheme.surface)
                                            .border(
                                                width = 1.dp,
                                                color = md_theme_surfaceCircle,
                                                shape = CircleShape
                                            )
                                    }
                                )
                                .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick),
                            contentAlignment = Alignment.Center
                        ) {
                            NavActionIcon(
                                icon = action.icon,
                                imageVector = action.imageVector,
                                iconTint = action.iconTint,
                                iconSize = action.iconSize,
                                contentDescription = action.title
                                    ?: action.titleRes?.let { titleRes -> stringResource(titleRes) }
                            )
                        }
                    }

                    is NavAction.Menu -> {
                        // Rendered below, within the overflow menu
                    }
                }
            }

            val contextMenu = navData.actions.filterIsInstance<NavAction.Menu>()

            if (contextMenu.isNotEmpty()) {
                // Keep the popup out of the Row, it would otherwise take part in its layout once open
                Box {
                    IconButton(modifier = Modifier.appTestTag("action_menu"), onClick = {
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
}

@Composable
private fun NavActionIcon(
    icon: DrawableResource?,
    imageVector: ImageVector?,
    iconTint: Color?,
    iconSize: Dp,
    contentDescription: String? = null,
) {
    val tint = iconTint ?: MaterialTheme.colorScheme.onSurface

    icon?.also {
        Icon(
            painter = painterResource(it),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }

    imageVector?.also {
        Icon(
            imageVector = it,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

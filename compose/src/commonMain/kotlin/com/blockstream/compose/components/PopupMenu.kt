package com.blockstream.compose.components

import com.blockstream.designsystem.components.AppNavBarIcon
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.adamglin.phosphoricons.light.Funnel
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.TextAa
import com.adamglin.phosphoricons.regular.Signature
import com.adamglin.phosphoricons.regular.Question
import com.adamglin.phosphoricons.regular.QrCode
import com.adamglin.phosphoricons.regular.Password
import com.adamglin.phosphoricons.regular.Key
import com.adamglin.phosphoricons.regular.BoxArrowDown
import com.adamglin.phosphoricons.regular.Binoculars
import com.adamglin.phosphoricons.regular.At
import com.blockstream.compose.navigation.MenuIcon
import com.blockstream.compose.events.Event
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_setup_guide
import blockstream_green.common.generated.resources.id_troubleshoot
import blockstream_green.common.generated.resources.id_about
import blockstream_green.common.generated.resources.id_app_settings
import blockstream_green.common.generated.resources.id_set_up_watchonly
import blockstream_green.common.generated.resources.note_pencil
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Eye
import com.adamglin.phosphoricons.regular.Funnel
import com.adamglin.phosphoricons.regular.Gear
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Sun
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.question
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
        fun from(navAction: NavAction.Menu, onAction: (Event) -> Unit, icon: (MenuIcon) -> ImageVector): MenuEntry {
            return MenuEntry(
                title = navAction.title.string(),
                imageVector = navAction.icon?.let(icon),
                testTag = navAction.testTag,
                enabled = navAction.enabled,
                onClick = { onAction(navAction.event()) }
            )
        }
    }
}

private fun MenuIcon.imageVector(): ImageVector = when (this) {
    MenuIcon.Info -> PhosphorIcons.Regular.Info
    MenuIcon.Rename -> PhosphorIcons.Regular.TextAa
    MenuIcon.WatchOnly -> PhosphorIcons.Regular.Binoculars
    MenuIcon.Archive -> PhosphorIcons.Regular.BoxArrowDown
    MenuIcon.Help -> PhosphorIcons.Regular.Question
    MenuIcon.Passphrase -> PhosphorIcons.Regular.Password
    MenuIcon.RecoveryPhrase -> PhosphorIcons.Regular.Key
    MenuIcon.Delete -> PhosphorIcons.Regular.Trash
    MenuIcon.RequestAmount -> PhosphorIcons.Regular.TextAa
    MenuIcon.Addresses -> PhosphorIcons.Regular.At
    MenuIcon.Sweep -> PhosphorIcons.Regular.QrCode
    MenuIcon.Sign -> PhosphorIcons.Regular.Signature
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
private val NAV_ACTION_TEXT_ICON_SIZE = 18.dp
private val NAV_ACTION_ICON_GLYPH_SIZE = 24.dp

@Composable
fun ActionMenu(
    navData: NavData,
    onAction: (Event) -> Unit,
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
                    is NavAction.Help -> {
                        val title = stringResource(Res.string.id_help)
                        LegacyTextAction(title = title, testTag = title, icon = Res.drawable.question, onClick = { onAction(action.event()) })
                    }

                    is NavAction.Note -> {
                        val title = stringResource(Res.string.id_add_note)
                        LegacyTextAction(title = title, testTag = title, icon = Res.drawable.note_pencil, onClick = { onAction(action.event()) })
                    }

                    is NavAction.InvoiceDescription -> {
                        val title = stringResource(Res.string.id_note)
                        LegacyTextAction(title = title, testTag = title, icon = Res.drawable.note_pencil, onClick = { onAction(action.event()) })
                    }

                    is NavAction.WatchOnly -> LegacyTextAction(
                        title = stringResource(Res.string.id_set_up_watchonly),
                        testTag = Res.string.id_set_up_watchonly.key,
                        imageVector = PhosphorIcons.Regular.Eye,
                        onClick = { onAction(action.event()) }
                    )

                    is NavAction.Troubleshoot -> {
                        val title = stringResource(Res.string.id_troubleshoot)
                        LegacyTextAction(title = title, testTag = title, onClick = { onAction(action.event()) })
                    }

                    is NavAction.SetupGuide -> {
                        val title = stringResource(Res.string.id_setup_guide)
                        LegacyTextAction(title = title, testTag = title, onClick = { onAction(action.event()) })
                    }

                    is NavAction.Region -> LegacyTextAction(title = action.name, testTag = action.name, onClick = { onAction(action.event()) })

                    is NavAction.About -> LegacyIconAction(
                        contentDescription = stringResource(Res.string.id_about),
                        testTag = action.testTag ?: Res.string.id_about.key,
                        imageVector = PhosphorIcons.Regular.Info,
                        onClick = { onAction(action.event()) }
                    )

                    is NavAction.AppSettings -> LegacyIconAction(
                        contentDescription = stringResource(Res.string.id_app_settings),
                        testTag = action.testTag ?: Res.string.id_app_settings.key,
                        imageVector = PhosphorIcons.Regular.Gear,
                        outlined = true,
                        onClick = { onAction(action.event()) }
                    )

                    is NavAction.Filter -> LegacyIconAction(
                        contentDescription = null,
                        testTag = action.testTag,
                        imageVector = if (action.isActive) PhosphorIcons.Light.Funnel else PhosphorIcons.Regular.Funnel,
                        iconSize = 22.dp,
                        active = action.isActive,
                        onClick = { onAction(action.event()) }
                    )

                    is NavAction.ThemeToggle -> {
                        val title = if (action.isLight) "Switch to dark" else "Switch to light"
                        LegacyIconAction(
                            contentDescription = title,
                            testTag = action.testTag ?: title,
                            imageVector = if (action.isLight) PhosphorIcons.Regular.Moon else PhosphorIcons.Regular.Sun,
                            onClick = { onAction(action.event()) }
                        )
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
                        MenuEntry.from(it, onAction) { icon -> icon.imageVector() }
                    })
                }
            }
        }
    }
}

@Composable
private fun LegacyTextAction(
    title: String,
    testTag: String?,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    imageVector: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.appTestTag(testTag),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavActionIcon(
                icon = icon,
                imageVector = imageVector,
                iconTint = null,
                iconSize = NAV_ACTION_TEXT_ICON_SIZE
            )
            Text(text = title, style = labelMedium)
        }
    }
}

@Composable
private fun LegacyIconAction(
    contentDescription: String?,
    testTag: String?,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    imageVector: ImageVector? = null,
    iconTint: Color? = null,
    iconSize: Dp = NAV_ACTION_ICON_GLYPH_SIZE,
    outlined: Boolean = false,
    active: Boolean = false
) {
    Box(
        modifier = Modifier
            .appTestTag(testTag)
            .padding(end = NAV_ACTION_ICON_END_PADDING)
            .size(NAV_ACTION_ICON_SIZE)
            .clip(CircleShape)
            .then(
                if (outlined) {
                    Modifier
                        .background(color = MaterialTheme.colorScheme.surface)
                        .border(width = 1.dp, color = md_theme_surfaceCircle, shape = CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (active && imageVector != null) {
            AppNavBarIcon(
                painter = rememberVectorPainter(imageVector),
                contentDescription = contentDescription,
                size = iconSize,
                active = true
            )
        } else {
            NavActionIcon(
                icon = icon,
                imageVector = imageVector,
                iconTint = iconTint,
                iconSize = iconSize,
                contentDescription = contentDescription
            )
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

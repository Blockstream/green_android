package com.blockstream.compose.designv2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.blockstream.compose.designv2.components.AppMenuItem
import com.blockstream.compose.designv2.components.AppMenu
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_filter
import blockstream_green.common.generated.resources.id_more_options
import blockstream_green.common.generated.resources.id_back
import com.adamglin.phosphoricons.light.Trash
import com.adamglin.phosphoricons.light.TextAa
import com.adamglin.phosphoricons.light.Signature
import com.adamglin.phosphoricons.light.QrCode
import com.adamglin.phosphoricons.light.Password
import com.adamglin.phosphoricons.light.Key
import com.adamglin.phosphoricons.light.BoxArrowDown
import com.adamglin.phosphoricons.light.Binoculars
import com.adamglin.phosphoricons.light.At
import com.blockstream.compose.navigation.MenuIcon
import com.blockstream.compose.extensions.icon
import blockstream_green.common.generated.resources.brand
import com.blockstream.compose.events.Event
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_edit_note
import blockstream_green.common.generated.resources.id_setup_guide
import blockstream_green.common.generated.resources.id_troubleshoot
import blockstream_green.common.generated.resources.id_about
import blockstream_green.common.generated.resources.id_app_settings
import blockstream_green.common.generated.resources.id_set_up_watchonly
import com.adamglin.phosphoricons.light.Funnel
import com.adamglin.phosphoricons.light.Gear
import com.adamglin.phosphoricons.light.Info
import com.adamglin.phosphoricons.light.Moon
import com.adamglin.phosphoricons.light.Sun
import androidx.compose.ui.graphics.vector.ImageVector
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_help
import com.adamglin.phosphoricons.light.Question
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.light.DotsThreeCircle
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.utils.appTestTag
import com.blockstream.designsystem.components.AppNavBarRegionButton
import com.blockstream.designsystem.components.AppNavBar
import com.blockstream.designsystem.components.AppNavBarIconButton
import com.blockstream.designsystem.components.AppNavBarIconButtonVariant
import com.blockstream.designsystem.components.AppNavBarTextAction
import com.blockstream.designsystem.components.AppNavBarTitle
import com.blockstream.designsystem.components.AppNavBarWalletChip
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopAppBarV2(
    navData: NavData,
    onAction: (Event) -> Unit,
    canGoBack: Boolean,
    goBack: () -> Unit,
    windowInsets: WindowInsets
) {
    if (!navData.isVisible) {
        AppNavBar(windowInsets = windowInsets)
        return
    }

    val navigator = LocalNavigator.current
    val title = navData.title?.string()
    val showBack = navData.showNavigationIcon && canGoBack && navData.walletName == null
    val onBack = { navData.backEvent?.let(onAction) ?: goBack() }

    AppNavBar(
        windowInsets = windowInsets,
        leading = {
            if (navData.showBrandLogo) {
                if (showBack) {
                    AppNavBarTitle(title = null, onBack = onBack, backContentDescription = stringResource(Res.string.id_back))
                }
                Image(painterResource(Res.drawable.brand), contentDescription = null, modifier = Modifier.height(28.dp))
            } else if (title != null || navData.subtitle != null || showBack) {
                AppNavBarTitle(
                    title = title,
                    subtitle = navData.subtitle?.string()?.takeIf { it.isNotBlank() },
                    icon = navData.titleNetwork?.let { painterResource(it.icon()) },
                    iconModifier = Modifier.appTestTag(navData.titleIconTestTag),
                    onBack = onBack.takeIf { showBack },
                    backContentDescription = stringResource(Res.string.id_back)
                )
            }
        },
        trailing = {
            navData.walletName?.takeIf { navData.showNavigationIcon }?.let { walletName ->
                AppNavBarWalletChip(
                    name = walletName,
                    onClick = { navigator.navigate(route = NavigateDestinations.Home) }
                )
            }
            NavActions(navData.actions, onAction)
        }
    )
}

@Composable
private fun NavActions(actions: List<NavAction>, onAction: (Event) -> Unit) {
    actions.forEach { action ->
        when (action) {
            is NavAction.Help -> {
                val title = stringResource(Res.string.id_help)
                IconAction(PhosphorIcons.Light.Question, title, testTag = title, onClick = { onAction(action.event()) })
            }

            is NavAction.Note -> NoteAction(
                hasNote = action.hasNote,
                testTag = stringResource(Res.string.id_add_note),
                onClick = { onAction(action.event()) }
            )

            is NavAction.InvoiceDescription -> NoteAction(
                hasNote = action.hasDescription,
                testTag = stringResource(Res.string.id_note),
                onClick = { onAction(action.event()) }
            )

            is NavAction.WatchOnly -> TextAction(
                title = stringResource(Res.string.id_set_up_watchonly),
                testTag = Res.string.id_set_up_watchonly.key,
                onClick = { onAction(action.event()) }
            )

            is NavAction.Troubleshoot -> {
                val title = stringResource(Res.string.id_troubleshoot)
                TextAction(title, testTag = title, onClick = { onAction(action.event()) })
            }

            is NavAction.SetupGuide -> {
                val title = stringResource(Res.string.id_setup_guide)
                TextAction(title, testTag = title, onClick = { onAction(action.event()) })
            }

            is NavAction.Region -> AppNavBarRegionButton(
                code = action.name,
                onClick = { onAction(action.event()) },
                modifier = Modifier.appTestTag(action.name)
            )

            is NavAction.About -> IconAction(
                icon = PhosphorIcons.Light.Info,
                contentDescription = stringResource(Res.string.id_about),
                testTag = action.testTag ?: Res.string.id_about.key,
                onClick = { onAction(action.event()) }
            )

            is NavAction.AppSettings -> IconAction(
                icon = PhosphorIcons.Light.Gear,
                contentDescription = stringResource(Res.string.id_app_settings),
                testTag = action.testTag ?: Res.string.id_app_settings.key,
                variant = AppNavBarIconButtonVariant.Bordered,
                onClick = { onAction(action.event()) }
            )

            is NavAction.Filter -> IconAction(
                icon = PhosphorIcons.Light.Funnel,
                contentDescription = stringResource(Res.string.id_filter),
                testTag = action.testTag,
                active = action.isActive,
                onClick = { onAction(action.event()) }
            )

            is NavAction.ThemeToggle -> {
                val title = if (action.isLight) "Switch to dark" else "Switch to light"
                IconAction(
                    icon = if (action.isLight) PhosphorIcons.Light.Moon else PhosphorIcons.Light.Sun,
                    contentDescription = title,
                    testTag = action.testTag ?: title,
                    onClick = { onAction(action.event()) }
                )
            }

            is NavAction.Menu -> Unit
        }
    }

    val menu = actions.filterIsInstance<NavAction.Menu>()
    if (menu.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            AppNavBarIconButton(
                icon = rememberVectorPainter(PhosphorIcons.Light.DotsThreeCircle),
                contentDescription = stringResource(Res.string.id_more_options),
                onClick = { expanded = true },
                modifier = Modifier.appTestTag("action_menu")
            )
            AppMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                menu.forEach { item ->
                    AppMenuItem(
                        text = item.title.string(),
                        icon = item.icon?.imageVectorV2(),
                        enabled = item.enabled,
                        modifier = Modifier.appTestTag(item.testTag),
                        onClick = {
                            expanded = false
                            onAction(item.event())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteAction(hasNote: Boolean, testTag: String, onClick: () -> Unit) {
    val title = stringResource(if (hasNote) Res.string.id_edit_note else Res.string.id_add_note)
    TextAction(title, testTag = testTag, onClick = onClick)
}

@Composable
private fun TextAction(title: String, testTag: String?, onClick: () -> Unit) {
    AppNavBarTextAction(text = title, onClick = onClick, modifier = Modifier.appTestTag(testTag))
}

@Composable
private fun IconAction(
    icon: ImageVector,
    contentDescription: String?,
    testTag: String?,
    onClick: () -> Unit,
    variant: AppNavBarIconButtonVariant = AppNavBarIconButtonVariant.Plain,
    active: Boolean = false
) {
    AppNavBarIconButton(
        icon = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        onClick = onClick,
        variant = variant,
        active = active,
        modifier = Modifier.appTestTag(testTag)
    )
}

private fun MenuIcon.imageVectorV2(): ImageVector = when (this) {
    MenuIcon.Info -> PhosphorIcons.Light.Info
    MenuIcon.Rename -> PhosphorIcons.Light.TextAa
    MenuIcon.WatchOnly -> PhosphorIcons.Light.Binoculars
    MenuIcon.Archive -> PhosphorIcons.Light.BoxArrowDown
    MenuIcon.Help -> PhosphorIcons.Light.Question
    MenuIcon.Passphrase -> PhosphorIcons.Light.Password
    MenuIcon.RecoveryPhrase -> PhosphorIcons.Light.Key
    MenuIcon.Delete -> PhosphorIcons.Light.Trash
    MenuIcon.RequestAmount -> PhosphorIcons.Light.TextAa
    MenuIcon.Addresses -> PhosphorIcons.Light.At
    MenuIcon.Sweep -> PhosphorIcons.Light.QrCode
    MenuIcon.Sign -> PhosphorIcons.Light.Signature
}

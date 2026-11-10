package com.blockstream.compose.designv2

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.fill.ArrowsDownUp
import com.adamglin.phosphoricons.fill.Gear
import com.adamglin.phosphoricons.fill.House
import com.adamglin.phosphoricons.fill.ShieldCheck
import com.adamglin.phosphoricons.light.ArrowsDownUp
import com.adamglin.phosphoricons.light.Gear
import com.adamglin.phosphoricons.light.House
import com.adamglin.phosphoricons.light.ShieldCheck
import com.blockstream.compose.navigation.TopLevelRoute
import com.blockstream.compose.navigation.TopLevelRoutes
import com.blockstream.designsystem.components.AppTabBar
import com.blockstream.designsystem.components.AppTabBarIconMotion
import com.blockstream.designsystem.components.AppTabBarItem
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavigationBarV2(
    isSelected: (TopLevelRoute) -> Boolean,
    onSelect: (TopLevelRoute) -> Unit,
    showSecurityBadge: Boolean
) {
    AppTabBar {
        TopLevelRoutes.forEach { topLevelRoute ->
            AppTabBarItem(
                selected = isSelected(topLevelRoute),
                onClick = { onSelect(topLevelRoute) },
                icon = topLevelRoute.icon(),
                selectedIcon = topLevelRoute.selectedIcon(),
                motion = topLevelRoute.motion(),
                label = stringResource(topLevelRoute.name),
                showBadge = topLevelRoute is TopLevelRoute.Security && showSecurityBadge,
                iconContentDescription = topLevelRoute.name.key
            )
        }
    }
}

private fun TopLevelRoute.icon(): ImageVector = when (this) {
    TopLevelRoute.Home -> PhosphorIcons.Light.House
    TopLevelRoute.Transact -> PhosphorIcons.Light.ArrowsDownUp
    TopLevelRoute.Security -> PhosphorIcons.Light.ShieldCheck
    TopLevelRoute.Settings -> PhosphorIcons.Light.Gear
}

private fun TopLevelRoute.selectedIcon(): ImageVector = when (this) {
    TopLevelRoute.Home -> PhosphorIcons.Fill.House
    TopLevelRoute.Transact -> PhosphorIcons.Fill.ArrowsDownUp
    TopLevelRoute.Security -> PhosphorIcons.Fill.ShieldCheck
    TopLevelRoute.Settings -> PhosphorIcons.Fill.Gear
}

private fun TopLevelRoute.motion(): AppTabBarIconMotion = when (this) {
    TopLevelRoute.Home, TopLevelRoute.Transact -> AppTabBarIconMotion.Pop
    TopLevelRoute.Security -> AppTabBarIconMotion.Wiggle
    TopLevelRoute.Settings -> AppTabBarIconMotion.Spin
}

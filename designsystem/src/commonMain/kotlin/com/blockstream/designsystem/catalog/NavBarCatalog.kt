package com.blockstream.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.light.Funnel
import com.adamglin.phosphoricons.light.Gear
import com.adamglin.phosphoricons.light.Info
import com.blockstream.designsystem.components.AppNavBar
import com.blockstream.designsystem.components.AppNavBarIconButton
import com.blockstream.designsystem.components.AppNavBarIconButtonVariant
import com.blockstream.designsystem.components.AppNavBarRegionButton
import com.blockstream.designsystem.components.AppNavBarTextAction
import com.blockstream.designsystem.components.AppNavBarTitle
import com.blockstream.designsystem.components.AppNavBarWalletChip

@Composable
fun NavBarCatalog() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Title")
            NavBar { AppNavBarTitle(title = "Transact") }
            NavBar { AppNavBarTitle(title = "App Settings", subtitle = "Applies to every wallet", onBack = {}, backContentDescription = "Back") }
            NavBar { AppNavBarTitle(title = null, subtitle = "My Wallet", onBack = {}, backContentDescription = "Back") }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Icon buttons")
            NavBar(
                leading = { AppNavBarTitle(title = "Actions") },
                trailing = {
                    AppNavBarIconButton(rememberVectorPainter(PhosphorIcons.Light.Info), contentDescription = "About", onClick = {})
                    AppNavBarIconButton(
                        rememberVectorPainter(PhosphorIcons.Light.Gear),
                        contentDescription = "Settings",
                        onClick = {},
                        variant = AppNavBarIconButtonVariant.Bordered
                    )
                    AppNavBarIconButton(rememberVectorPainter(PhosphorIcons.Light.Funnel), contentDescription = "Filter", onClick = {}, active = true)
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Trailing items")
            NavBar(leading = { AppNavBarTitle(title = "Send", onBack = {}, backContentDescription = "Back") }, trailing = { AppNavBarTextAction(text = "Add Note", onClick = {}) })
            NavBar(leading = { AppNavBarTitle(title = "Send", onBack = {}, backContentDescription = "Back") }, trailing = { AppNavBarTextAction(text = "Add Note", onClick = {}, enabled = false) })
            NavBar(leading = { AppNavBarTitle(title = "Home") }, trailing = { AppNavBarWalletChip(name = "My Wallet with a long name", onClick = {}) })
            NavBar(leading = { AppNavBarTitle(title = "Buy", onBack = {}, backContentDescription = "Back") }, trailing = { AppNavBarRegionButton(code = "UA", onClick = {}) })
        }
    }
}

@Composable
private fun NavBar(
    trailing: @Composable RowScope.() -> Unit = {},
    leading: @Composable RowScope.() -> Unit
) {
    AppNavBar(windowInsets = WindowInsets(0), leading = leading, trailing = trailing)
}

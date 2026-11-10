package com.blockstream.compose.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.blockstream.compose.theme.bodyMedium
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavigationBar(
    isSelected: (TopLevelRoute) -> Boolean,
    onSelect: (TopLevelRoute) -> Unit,
    showSecurityBadge: Boolean
) {
    NavigationBar {
        TopLevelRoutes.forEach { topLevelRoute ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (topLevelRoute is TopLevelRoute.Security && showSecurityBadge) {
                                Badge()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = topLevelRoute.icon,
                            contentDescription = topLevelRoute.name.key
                        )
                    }
                },
                label = {
                    Text(stringResource(topLevelRoute.name), style = bodyMedium)
                },
                selected = isSelected(topLevelRoute),
                onClick = { onSelect(topLevelRoute) }
            )
        }
    }
}

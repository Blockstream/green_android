package com.blockstream.compose.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    gesturesEnabled: Boolean = true,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
            ) {
                drawerContent()
            }
        },
        content = content
    )
}
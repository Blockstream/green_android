@file:OptIn(ExperimentalComposeUiApi::class)

package com.blockstream.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState

@Stable
class NavDataState(navData: NavData = NavData()) {
    var data = mutableStateOf(navData)
        private set

    fun update(data: NavData) {
        this.data.value = data
    }
}

val LocalNavData = compositionLocalOf { NavDataState() }

@Composable
fun AppBarState(viewModel: INavData) {
    val navigator = LocalNavigator.current
    val navDataState = LocalNavData.current
    val selfBackStackEntry = LocalNavBackStackEntry.current

    val navData by viewModel.navData.collectAsStateWithLifecycle()
    val currentBackStackEntry by navigator.currentBackStackEntryAsState()

    val key = navData.hashCode() + (currentBackStackEntry?.id?.hashCode() ?: 0)

    LaunchedEffect(key) {
        if (currentBackStackEntry?.id == selfBackStackEntry?.id) {
            navDataState.update(navData)
        }
    }

    BackHandler(enabled = !navData.isVisible || navData.backHandlerEnabled) {
        viewModel.navigateBack()
    }
}
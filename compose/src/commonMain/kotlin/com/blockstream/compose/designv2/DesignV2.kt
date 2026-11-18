package com.blockstream.compose.designv2

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.navigation.LocalNavBackStackEntry
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.compose.navigation.LocalScreenNavData
import com.blockstream.compose.navigation.NavDataState
import com.blockstream.compose.utils.excludeTop
import com.blockstream.data.config.AppInfo
import com.blockstream.data.managers.SettingsManager
import com.blockstream.designsystem.AppTheme

val LocalDesignV2 = staticCompositionLocalOf { false }

@Composable
fun rememberDesignV2Enabled(appInfo: AppInfo, settingsManager: SettingsManager): Boolean {
    if (!appInfo.isDevelopmentOrDebug) return false

    val enabled by remember(settingsManager) { settingsManager.isDesignV2EnabledFlow() }
        .collectAsStateWithLifecycle(settingsManager.isDesignV2Enabled())
    return enabled
}

@Composable
fun DesignVersion(v1: @Composable () -> Unit, v2: @Composable () -> Unit) {
    if (LocalDesignV2.current) v2() else v1()
}

@Composable
@ReadOnlyComposable
fun <T> designVersion(v1: T, v2: T): T = if (LocalDesignV2.current) v2 else v1

@Composable
fun ScreenSurface(content: @Composable () -> Unit) {
    val designV2 = LocalDesignV2.current
    val navigator = LocalNavigator.current
    val entry = LocalNavBackStackEntry.current
    val innerPadding = LocalInnerPadding.current
    val screenNavData = viewModel { ScreenNavDataHolder() }.state
    val canGoBack = remember(entry) {
        navigator.currentBackStack.value.indexOfFirst { it.id == entry?.id } > 1
    }

    CompositionLocalProvider(LocalScreenNavData provides screenNavData) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (designV2) Modifier.background(AppTheme.colors.surface.canvas) else Modifier)
        ) {
            if (designV2) {
                TopAppBarV2(
                    navData = screenNavData.data.value,
                    onAction = screenNavData.onEvent,
                    canGoBack = canGoBack,
                    goBack = { navigator.navigateUp() },
                    windowInsets = WindowInsets(top = innerPadding.calculateTopPadding())
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalInnerPadding provides if (designV2) innerPadding.excludeTop() else innerPadding,
                    content = content
                )
            }
        }
    }
}

private class ScreenNavDataHolder : ViewModel() {
    val state = NavDataState()
}

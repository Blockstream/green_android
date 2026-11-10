package com.blockstream.compose.designv2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (LocalDesignV2.current) Modifier.background(AppTheme.colors.surface.canvas) else Modifier)
    ) {
        content()
    }
}

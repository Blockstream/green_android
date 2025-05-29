package com.blockstream.green.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ca.gosyer.appdirs.AppDirs
import co.touchlab.kermit.Logger
import com.blockstream.common.data.AppConfig
import com.blockstream.common.models.MainViewModel
import com.blockstream.compose.GreenApp
import com.blockstream.compose.di.initKoinDesktop
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId
import com.blockstream.green.data.config.AppInfo

fun main() = application {

    setupDesktop()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Blockstream App",
    ) {
        DesktopApp()
    }
}

fun setupDesktop() {
    val appDirs = AppDirs("Green", "Blockstream")

    val appConfig = AppConfig.default(
        isDebug = true,
        filesDir = appDirs.getUserDataDir(),
        cacheDir = appDirs.getUserCacheDir(),
        analyticsFeatureEnabled = false,
        lightningFeatureEnabled = false,
        storeRateEnabled = false
    )

    val appInfo = AppInfo(userAgent = "green_ios", "version", isDebug = true, isDevelopment = true)

    initKoinDesktop(
        appConfig = appConfig,
        appInfo = appInfo,
        doOnStartup = {
            Logger.d { "Start up" }
        }
    )
}

@Composable
fun DesktopApp() {
    val mainViewModel = remember { MainViewModel() }

    GreenChrome()
    GreenTheme {
        GreenApp(
            mainViewModel = mainViewModel,
            modifier = Modifier.compatTestTagsAsResourceId()
        )
    }
}
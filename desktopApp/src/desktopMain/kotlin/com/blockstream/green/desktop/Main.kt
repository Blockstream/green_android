package com.blockstream.green.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ca.gosyer.appdirs.AppDirs
import com.blockstream.compose.GreenApp
import com.blockstream.compose.di.initKoinDesktop
import com.blockstream.compose.models.MainViewModel
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId
import com.blockstream.data.BuildConfig
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.AppConfig

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
        storeRateEnabled = false,
        greenlightCert = BuildConfig.GREENLIGHT_CERT,
        greenlightKey = BuildConfig.GREENLIGHT_KEY
    )

    val appInfo = AppInfo(
        userAgent = "green_desktop",
        version = "0.0.0",
        isDebug = true,
        isDevelopment = true,
        developmentPin = BuildConfig.DEV_PIN_CODE.takeIf { appConfig.isDebug }
    )

    initKoinDesktop(
        appConfig = appConfig,
        appInfo = appInfo
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
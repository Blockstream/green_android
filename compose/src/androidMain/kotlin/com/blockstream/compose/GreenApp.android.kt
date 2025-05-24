package com.blockstream.compose

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.managers.LocaleManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.green.data.config.AppInfo
import com.blockstream.ui.navigation.LocalNavigator
import com.blockstream.ui.navigation.bottomsheet.rememberBottomSheetNavigator
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

@Composable
fun GreenAndroidPreview(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // startKoin only once
    KoinPlatformTools.defaultContext().getOrNull() ?: startKoin {
        modules(module {
            single { context }
            single {
                BluetoothManager(get(), null)
            }
            single {
                LocaleManager(get())
            }
            single {
                LifecycleManager(get(), get())
            }
            single<GreenKeystore> {
                AndroidKeystore(androidContext())
            }
            single<ObservableSettings> {
                val sharedPreferences = androidContext().getSharedPreferences(
                    SettingsManager.APPLICATION_SETTINGS_NAME,
                    Context.MODE_PRIVATE
                )
                SharedPreferencesSettings(sharedPreferences)
            }
            single {
                SettingsManager(
                    settings = get(),
                    analyticsFeatureEnabled = true,
                    lightningFeatureEnabled = true,
                    storeRateEnabled = true
                )
            }
            single {
                AppInfo(
                    userAgent = "GreenAndroidPreview",
                    version = "1.0.0-preview",
                    isDebug = true,
                    isDevelopment = false
                )
            }
        })
    }


    val dialogState = remember { DialogState() }
    val platformManager = rememberPlatformManager()
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    GreenTheme {
        CompositionLocalProvider(
            LocalDialog provides dialogState,
            LocalPlatformManager provides platformManager,
            LocalNavigator provides navController,
            LocalPreview provides true
        ) {
            DialogHost(state = dialogState)

            Scaffold(content = { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    content()
                }
            })

        }
    }
}

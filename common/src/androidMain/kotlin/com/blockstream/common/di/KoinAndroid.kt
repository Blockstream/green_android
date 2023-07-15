package com.blockstream.common.di

import android.content.Context
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.database.DriverFactory
import com.blockstream.common.managers.SettingsManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoinAndroid(
    appConfig: AppConfig,
    appInfo: AppInfo,
    vararg appModule: Module,
    doOnStartup: () -> Unit
): KoinApplication = initKoin(appConfig, *appModule, module {
    single {
        appInfo
    }
    single {
        doOnStartup
    }
})

actual val platformModule: Module = module {
    single {
        DriverFactory(androidContext())
    }
    single<Settings> {
        val sharedPreferences = androidContext().getSharedPreferences(
            SettingsManager.APPLICATION_SETTINGS_NAME,
            Context.MODE_PRIVATE
        )
        SharedPreferencesSettings(sharedPreferences)
    }
}

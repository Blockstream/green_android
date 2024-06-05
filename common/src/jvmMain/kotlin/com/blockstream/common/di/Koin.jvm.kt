package com.blockstream.common.di

import com.blockstream.common.database.DriverFactory
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule = module {
    single {
        DriverFactory()
    }
    single<Settings> {
        val preferences : Preferences = Preferences.userRoot()
        PreferencesSettings(preferences)
    }
}


package com.blockstream.common.di

import com.blockstream.common.database.DriverFactory
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.LocaleManager
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule = module {
    single {
        DriverFactory()
    }
    single {
        LocaleManager()
    }
    single<ObservableSettings> {
        val preferences : Preferences = Preferences.userRoot()
        PreferencesSettings(preferences)
    }
    single<BluetoothManager> { BluetoothManager() }
}


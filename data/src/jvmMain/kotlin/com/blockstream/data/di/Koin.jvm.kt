package com.blockstream.data.di

import com.blockstream.data.database.DriverFactory
import com.blockstream.data.managers.BluetoothManager
import com.blockstream.data.managers.LocaleManager
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
        val preferences: Preferences = Preferences.userRoot()
        PreferencesSettings(preferences)
    }
    single<BluetoothManager> { BluetoothManager() }
}


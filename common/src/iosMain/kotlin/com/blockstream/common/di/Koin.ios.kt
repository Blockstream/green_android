package com.blockstream.common.di

import com.blockstream.common.CountlyBase
import com.blockstream.common.CountlyIOS
import com.blockstream.common.database.DriverFactory
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.LocaleManager
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule = module {
    single {
        DriverFactory()
    }
    single<CountlyBase> {
        CountlyIOS(get(), get(), get(), get())
    }
    single {
        LocaleManager()
    }
    single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults()) }

    single<BluetoothManager> { BluetoothManager() }
}
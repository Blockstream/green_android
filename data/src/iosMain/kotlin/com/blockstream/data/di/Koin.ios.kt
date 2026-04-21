package com.blockstream.data.di

import com.blockstream.data.CountlyBase
import com.blockstream.data.CountlyIOS
import com.blockstream.data.database.DriverFactory
import com.blockstream.data.managers.BluetoothManager
import com.blockstream.data.managers.LocaleManager
import com.blockstream.data.walletabi.walletconnect.NoOpWalletAbiWalletConnectBridge
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectBridge
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
    single<WalletAbiWalletConnectBridge> { NoOpWalletAbiWalletConnectBridge() }

    single<BluetoothManager> { BluetoothManager() }
}

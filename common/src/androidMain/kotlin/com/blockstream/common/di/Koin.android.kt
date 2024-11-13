package com.blockstream.common.di

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.preference.PreferenceManager
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.database.DriverFactory
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.AndroidKeystore
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        DriverFactory(androidContext())
    }
    single {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }
    single<Settings> {
        val sharedPreferences = androidContext().getSharedPreferences(
            SettingsManager.APPLICATION_SETTINGS_NAME,
            Context.MODE_PRIVATE
        )
        SharedPreferencesSettings(sharedPreferences)
    }
    single {
        AndroidKeystore(androidContext())
    } binds (arrayOf(GreenKeystore::class))

    single<BluetoothManager> { BluetoothManager(androidContext(), get()) }
    single<BluetoothAdapter?> { (androidContext().getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter }
}
package com.blockstream.common.di

import co.touchlab.kermit.Logger
import com.blockstream.common.CountlyBase
import com.blockstream.common.CountlyIOS
import com.blockstream.common.database.DriverFactory
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.LocaleManager
import com.blockstream.common.managers.LifecycleManager
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
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
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults()) }

    single<BluetoothManager> { BluetoothManager() }
}


// Access from Swift to create a logger
@Suppress("unused")
fun Koin.loggerWithTag(tag: String) =
    get<Logger>(qualifier = null) { parametersOf(tag) }

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getLifecycleManager()= getKoin().get<LifecycleManager>()
}

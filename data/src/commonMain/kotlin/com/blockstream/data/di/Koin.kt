package com.blockstream.data.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.chunked
import co.touchlab.kermit.platformLogWriter
import com.blockstream.data.data.AppConfig
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.getGdkBinding
import com.blockstream.data.gdk.getWally
import com.blockstream.data.gdk.params.InitConfig
import com.blockstream.data.lightning.GreenlightKeys
import com.blockstream.data.lightning.LightningManager
import com.blockstream.data.lwk.LwkManager
import com.blockstream.data.managers.AssetManager
import com.blockstream.data.managers.LifecycleManager
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.managers.WalletSettingsManager
import kotlinx.coroutines.MainScope
import okio.internal.commonToUtf8String
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.io.encoding.Base64

typealias ApplicationScope = kotlinx.coroutines.CoroutineScope

expect val platformModule: Module



@OptIn(ExperimentalUnsignedTypes::class)
fun commonModules(appConfig: AppConfig): List<Module> {
    return listOf(module {
        single {
            appConfig
        }
        single<ApplicationScope> {
            MainScope()
        }
        single {
            getWally()
        }
        single {
            AssetManager
        }
        single {
            LifecycleManager(get(), get())
        }
        single {
            Database(get(), get())
        }
        single {
            PromoManager(get(), get(), get())
        }
        single {
            SettingsManager(
                settings = get(),
                analyticsFeatureEnabled = appConfig.analyticsFeatureEnabled,
                lightningFeatureEnabled = appConfig.lightningFeatureEnabled,
                storeRateEnabled = appConfig.storeRateEnabled
            )
        }
        single {
            val config = InitConfig(
                datadir = appConfig.filesDir,
                logLevel = if (appConfig.isDebug) "debug" else "none"
            )
            Gdk(
                settings = get(),
                gdkBinding = getGdkBinding(printGdkMessages = appConfig.isDebug, config = config)
            )
        }
        single {
            val greenlightKeys = GreenlightKeys(
                breezApiKey = appConfig.breezApiKey?.let { base64 ->
                    Base64.decode(base64).commonToUtf8String()
                } ?: "",
                deviceKey = appConfig.greenlightKey?.takeIf { it.isNotBlank() }?.let {
                    Base64.decode(it).toUByteArray().toTypedArray()
                        .toList()
                },
                deviceCert = appConfig.greenlightCert?.takeIf { it.isNotBlank() }?.let {
                    Base64.decode(it).toUByteArray().toTypedArray()
                        .toList()
                }
            )

            LightningManager(greenlightKeys, get(), get(), get(), get(), get())
        }
        singleOf(::WalletSettingsManager)
        singleOf(::SessionManager)
        singleOf(::LwkManager)

        // Set minSeverity to Global Logger
        Logger.setMinSeverity(if (appConfig.isDebug) Severity.Debug else Severity.Info)
        Logger.setLogWriters(platformLogWriter().chunked())

        factory { (tag: String?) -> if (tag != null) Logger.withTag(tag) else Logger }
    })
}



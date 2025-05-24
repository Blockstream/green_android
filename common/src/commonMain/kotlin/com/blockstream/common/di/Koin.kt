package com.blockstream.common.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.chunked
import co.touchlab.kermit.platformLogWriter
import com.blockstream.common.btcpricehistory.btcPriceHistoryModule
import com.blockstream.common.data.AppConfig
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.getGdkBinding
import com.blockstream.common.gdk.getWally
import com.blockstream.common.gdk.params.InitConfig
import com.blockstream.common.lightning.GreenlightKeys
import com.blockstream.common.lightning.LightningManager
import com.blockstream.common.managers.AssetManager
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.managers.PromoManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.data.config.AppInfo
import kotlinx.coroutines.MainScope
import okio.internal.commonToUtf8String
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.io.encoding.Base64

typealias ApplicationScope = kotlinx.coroutines.CoroutineScope

expect val platformModule: Module

fun initKoin(appInfo: AppInfo, appConfig: AppConfig, doOnStartup: () -> Unit = {}, vararg appModules: Module): KoinApplication {
    val koinApplication = startKoin {
        modules(
            module {

                single {
                    appInfo
                }
                single {
                    doOnStartup
                }
            }
        )
        modules(commonModule)
        modules(*appModules)
        modules(platformModule)
        modules(commonModules(appConfig))
        modules(btcPriceHistoryModule)
    }

    // Dummy initialization logic, making use of appModule declarations for demonstration purposes.
    val koin = koinApplication.koin
    // doOnStartup is a lambda which is implemented in Swift on iOS side
    val doOnStartup = koin.get<() -> Unit>()
    doOnStartup.invoke()

    val logger = koin.get<Logger> { parametersOf(null) }
    val appInfo = koin.get<AppInfo>()
    logger.v { "Green: version: ${appInfo.version}" }

    return koinApplication
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun commonModules(appConfig: AppConfig): List<Module> {
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
            SessionManager(get(), get(), get(), get(), get(), get(), get(), get())
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

        // Set minSeverity to Global Logger
        Logger.setMinSeverity(if (appConfig.isDebug) Severity.Debug else Severity.Info)
        Logger.setLogWriters(platformLogWriter().chunked())

        factory { (tag: String?) -> if (tag != null) Logger.withTag(tag) else Logger }
    }, factoryViewModels)
}



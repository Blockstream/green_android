package com.blockstream.common.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.getGdkBinding
import com.blockstream.common.gdk.getWally
import com.blockstream.common.gdk.params.InitConfig
import com.blockstream.common.lightning.GreenlightKeys
import com.blockstream.common.lightning.LightningManager
import com.blockstream.common.managers.AssetManager
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import kotlinx.coroutines.MainScope
import okio.internal.commonToUtf8String
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.io.encoding.Base64

typealias ApplicationScope = kotlinx.coroutines.CoroutineScope

fun initKoin(appConfig: AppConfig, vararg appModules: Module): KoinApplication {
    val koinApplication = startKoin {
        modules(*appModules)
        modules(platformModule)
        modules(commonModules(appConfig))
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
            LifecycleManager(get(), get(), get())
        }
        single {
            Database(get(), get())
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
                datadir = appConfig.gdkDataDir,
                logLevel = if (appConfig.isDebug) "debug" else "none"
            )
            Gdk(
                settings = get(),
                gdkBinding = getGdkBinding(appConfig.isDebug, config)
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
            LightningManager(get(), greenlightKeys)
        }

        val minSeverity = if (appConfig.isDebug) Severity.Debug else Severity.Info

        // Set minSeverity to Global Logger
        Logger.setMinSeverity(minSeverity)

        val baseLogger = Logger(
            config = StaticConfig(
                minSeverity = minSeverity,
                logWriterList = listOf(platformLogWriter())
            ), "Green"
        )

        factory { (tag: String?) -> if (tag != null) baseLogger.withTag(tag) else baseLogger }
    })
}

expect val platformModule: Module

package com.blockstream.data.di

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
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable.Companion.FILE_LOG_QUALIFIER
import kotlinx.coroutines.MainScope
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
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
            LifecycleManager(get(), get())
        }
        single {
            Database(get(), get())
        }
        single {
            PromoManager(get(), get())
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
                gdkBinding = getGdkBinding(
                    printGdkMessages = appConfig.isDebug,
                    config = config,
                    logger = get(qualifier = named(FILE_LOG_QUALIFIER)) {
                        parametersOf("GDK", LogBucket.Gdk)
                    }
                )
            )
        }
        single {
            val greenlightKeys = GreenlightKeys(
                deviceKey = appConfig.greenlightKey?.takeIf { it.isNotBlank() }?.let {
                    Base64.decode(it)
                },
                deviceCert = appConfig.greenlightCert?.takeIf { it.isNotBlank() }?.let {
                    Base64.decode(it)
                }
            )

            LightningManager(greenlightKeys, get(), get(), get(), get(), get(), get())
        }
        singleOf(::WalletSettingsManager)
        singleOf(::SessionManager)
        singleOf(::LwkManager)
        singleOf(::AssetManager)
        factoryOf(::NetworkAssetManager)
    })
}


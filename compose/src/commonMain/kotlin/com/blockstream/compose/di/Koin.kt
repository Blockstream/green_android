package com.blockstream.compose.di

import co.touchlab.kermit.Logger
import com.blockstream.common.btcpricehistory.btcPriceHistoryModule
import com.blockstream.common.data.AppConfig
import com.blockstream.common.di.commonModule
import com.blockstream.common.di.commonModules
import com.blockstream.common.di.platformModule
import com.blockstream.compose.navigation.NavigateToWallet
import com.blockstream.green.data.config.AppInfo
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

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
                singleOf(::NavigateToWallet)
            }
        )
        modules(*appModules)
        modules(commonModules(appConfig))
        modules(commonModule)
        modules(platformModule)
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
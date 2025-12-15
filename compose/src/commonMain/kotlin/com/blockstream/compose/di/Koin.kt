package com.blockstream.compose.di

import co.touchlab.kermit.Logger
import com.blockstream.compose.navigation.NavigateToWallet
import com.blockstream.data.btcpricehistory.btcPriceHistoryModule
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.AppConfig
import com.blockstream.data.di.commonModule
import com.blockstream.data.di.commonModules
import com.blockstream.data.di.platformModule
import com.blockstream.domain.domainModule
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
        modules(domainModule)
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
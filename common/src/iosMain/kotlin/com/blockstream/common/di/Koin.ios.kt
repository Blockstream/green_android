package com.blockstream.common.di

import co.touchlab.kermit.Logger
import com.blockstream.common.CountlyBase
import com.blockstream.common.CountlyIOS
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.NoKeystore
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.DriverFactory
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.fcm.Firebase
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.common.managers.*
import com.blockstream.common.managers.DeviceManager.Companion.JADE
import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.data.notifications.models.MeldNotificationData
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import platform.Foundation.*

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

@OptIn(ExperimentalForeignApi::class)
fun startKoin(doOnStartup: () -> Unit = {}) {
    val applicationSupportDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )

    val cachesDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSCachesDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )

    val version =
        NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "0.0.0"

    val appConfig = AppConfig.default(
        isDebug = true,
        filesDir = applicationSupportDirectory?.path ?: "",
        cacheDir = cachesDirectory?.path ?: "",
        analyticsFeatureEnabled = true,
        lightningFeatureEnabled = true,
        storeRateEnabled = false
    )

    val appInfo = AppInfo(userAgent = "green_ios", version, isDebug = true, isDevelopment = true)

    initKoin(
        appInfo = appInfo, appConfig = appConfig, doOnStartup = doOnStartup, module {
            single<GreenKeystore> {
                NoKeystore()
            }
            single {
                ZendeskSdk()
            }
            single {
                Firebase()
            }
            single {
                NotificationManager()
            }
            single {
                DeviceManager(
                    get(), get(), get(), listOf(JADE)
                )
            }
//            single {
//                DeviceConnectionManager(
//                    get(), get(),get()
//                )
//            } binds (arrayOf(DeviceConnectionManager::class, DeviceConnectionInterface::class))
            single<FcmCommon> {
                object : FcmCommon(get()) {
                    override fun showBuyTransactionNotification(meldNotificationData: MeldNotificationData) {
                        //no-op
                    }

                    override fun scheduleLightningBackgroundJob(
                        walletId: String, breezNotification: BreezNotification
                    ) {

                    }

                    override suspend fun showLightningPaymentNotification(
                        wallet: GreenWallet, paymentHash: String, satoshi: Long
                    ) {

                    }

                    override suspend fun showOpenWalletNotification(
                        wallet: GreenWallet, breezNotification: BreezNotification
                    ) {

                    }

                    override fun showDebugNotification(title: String, message: String) {

                    }

                }
            }
        })
}

// Access from Swift to create a logger
@Suppress("unused")
fun Koin.loggerWithTag(tag: String) = get<Logger>(qualifier = null) { parametersOf(tag) }

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getLifecycleManager() = getKoin().get<LifecycleManager>()
}

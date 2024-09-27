package com.blockstream.compose.di

import com.blockstream.common.ZendeskSdk
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.NoKeystore
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.di.initKoin
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.fcm.Firebase
import com.blockstream.common.interfaces.DeviceConnectionInterface
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.managers.DeviceManager.Companion.JADE
import com.blockstream.compose.managers.DeviceConnectionManager
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.binds
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

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

    val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "0.0.0"

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
        appInfo = appInfo,
        appConfig = appConfig,
        doOnStartup = doOnStartup,
        module {
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
                DeviceManager(
                    get(),
                    get(),
                    get(),
                    listOf(JADE)
                )
            }
            single {
                DeviceConnectionManager(
                    get(), get(),get()
                )
            } binds (arrayOf(DeviceConnectionManager::class, DeviceConnectionInterface::class))
            single<FcmCommon> {
                object : FcmCommon(get()){
                    override fun showDebugNotification(title: String, message: String) {

                    }

                    override fun scheduleLightningBackgroundJob(
                        walletId: String,
                        breezNotification: BreezNotification
                    ) {

                    }

                    override suspend fun showLightningPaymentNotification(
                        wallet: GreenWallet,
                        paymentHash: String,
                        satoshi: Long
                    ) {

                    }

                    override suspend fun showOpenWalletNotification(
                        wallet: GreenWallet,
                        breezNotification: BreezNotification
                    ) {

                    }

                }
            }
        }
    )
}
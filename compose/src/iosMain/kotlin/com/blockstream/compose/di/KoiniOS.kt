@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.compose.di

import com.blockstream.compose.managers.DeviceConnectionManager
import com.blockstream.data.ZendeskSdk
import com.blockstream.data.config.AppInfo
import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.crypto.NoKeystore
import com.blockstream.data.data.AppConfig
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.fcm.Firebase
import com.blockstream.data.interfaces.DeviceConnectionInterface
import com.blockstream.data.lightning.BreezNotification
import com.blockstream.data.managers.DeviceManager
import com.blockstream.data.managers.NotificationManager
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import com.blockstream.jade.connection.JadeBleConnection
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.binds
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.uuid.ExperimentalUuidApi

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
        storeRateEnabled = false,
        appKeysString = null
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
                NotificationManager()
            }
            single {
                DeviceManager(
                    get(),
                    get(),
                    get(),
                    listOf(JadeBleConnection.JADE_SERVICE)
                )
            }
            single {
                DeviceConnectionManager(
                    get(), get(), get()
                )
            } binds (arrayOf(DeviceConnectionManager::class, DeviceConnectionInterface::class))
            single<FcmCommon> {
                object : FcmCommon(get()) {
                    override fun showDebugNotification(title: String, message: String) {

                    }

                    override fun showBuyTransactionNotification(meldNotificationData: MeldNotificationData) {
                        //no-op
                    }

                    override fun scheduleLightningBackgroundJob(
                        walletId: String,
                        breezNotification: BreezNotification
                    ) {

                    }

                    override fun scheduleBoltzBackgroundJob(boltzNotificationData: BoltzNotificationSimple) {

                    }

                    override suspend fun showSwapPaymentReceivedNotification(wallet: GreenWallet) {

                    }

                    override suspend fun showSwapPaymentSentNotification(wallet: GreenWallet) {

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

package com.blockstream

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.blockstream.common.CountlyBase
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.di.initKoin
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.green.data.config.AppInfo
import org.junit.BeforeClass
import org.koin.dsl.module
import org.koin.test.KoinTest

abstract class BaseTest : KoinTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {

            val context = ApplicationProvider.getApplicationContext<Context>()

            val appInfo = AppInfo(
                userAgent = "green_android",
                version = "",
                isDebug = false,
                isDevelopment = false
            )

            val appConfig = AppConfig.default(
                isDebug = true,
                filesDir = context.filesDir.absolutePath,
                cacheDir = context.cacheDir.absolutePath,
                analyticsFeatureEnabled = false,
                lightningFeatureEnabled = false,
                storeRateEnabled = false
            )

            initKoin(
                appInfo = appInfo,
                appConfig = appConfig,
                doOnStartup = {

                },
                module {
                    single {
                        context
                    }

                    single<FcmCommon> {
                        object : FcmCommon(get()) {
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

                    single<CountlyBase> {
                        object : CountlyBase(get(), get(), get(), get()) {
                            override fun updateRemoteConfig(force: Boolean) {

                            }

                            override fun updateOffset() {

                            }

                            override fun updateDeviceId() {
                            }

                            override fun updateConsent(withUserConsent: Boolean) {

                            }

                            override fun viewRecord(
                                viewName: String,
                                segmentation: Map<String, Any>?
                            ) {
                            }

                            override fun eventRecord(
                                key: String,
                                segmentation: Map<String, Any>?
                            ) {
                            }

                            override fun eventStart(key: String) {

                            }

                            override fun eventCancel(key: String) {

                            }

                            override fun eventEnd(
                                key: String,
                                segmentation: Map<String, Any>?
                            ) {
                                TODO("Not yet implemented")
                            }

                            override fun traceStart(key: String) {
                                TODO("Not yet implemented")
                            }

                            override fun traceEnd(key: String) {
                                TODO("Not yet implemented")
                            }

                            override fun setProxy(proxyUrl: String?) {
                                TODO("Not yet implemented")
                            }

                            override fun updateUserWallets(wallets: Int) {
                                TODO("Not yet implemented")
                            }

                            override fun getRemoteConfigValueAsString(key: String): String? {
                                TODO("Not yet implemented")
                            }

                            override fun getRemoteConfigValueAsBoolean(key: String): Boolean? {
                                TODO("Not yet implemented")
                            }

                            override fun getRemoteConfigValueAsNumber(key: String): Long? {
                                TODO("Not yet implemented")
                            }

                            override fun recordExceptionImpl(throwable: Throwable) {
                                TODO("Not yet implemented")
                            }

                            override fun recordFeedback(
                                rating: Int,
                                email: String?,
                                comment: String
                            ) {
                                TODO("Not yet implemented")
                            }

                        }
                    }
                }
            )
        }
    }
}
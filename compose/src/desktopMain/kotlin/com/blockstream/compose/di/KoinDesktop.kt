@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.compose.di

import com.blockstream.data.CountlyBase
import com.blockstream.data.ZendeskSdk
import com.blockstream.data.config.AppInfo
import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.crypto.NoKeystore
import com.blockstream.data.data.AppConfig
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.fcm.Firebase
import com.blockstream.data.lightning.BreezNotification
import com.blockstream.data.managers.BluetoothManager
import com.blockstream.data.managers.DeviceManager
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi

fun initKoinDesktop(appConfig: AppConfig, appInfo: AppInfo, doOnStartup: () -> Unit = {}) {

    initKoin(
        appInfo = appInfo,
        appConfig = appConfig,
        doOnStartup = doOnStartup,
        module {
            single<CountlyBase> {
                // Dummy
                object : CountlyBase(get(), get(), get(), get()) {
                    override fun updateRemoteConfig(force: Boolean) {

                    }

                    override fun updateOffset() {
                    }

                    override fun updateDeviceId() {
                    }

                    override fun updateConsent(withUserConsent: Boolean) {
                    }

                    override fun viewRecord(viewName: String, segmentation: Map<String, Any>?) {
                    }

                    override fun eventRecord(key: String, segmentation: Map<String, Any>?) {
                    }

                    override fun eventStart(key: String) {
                    }

                    override fun eventCancel(key: String) {
                    }

                    override fun eventEnd(key: String, segmentation: Map<String, Any>?) {
                    }

                    override fun traceStart(key: String) {
                    }

                    override fun traceEnd(key: String) {
                    }

                    override fun setProxy(proxyUrl: String?) {
                    }

                    override fun updateUserWallets(wallets: Int) {
                    }

                    override fun getRemoteConfigValueAsString(key: String) = null

                    override fun getRemoteConfigValueAsBoolean(key: String) = null

                    override fun getRemoteConfigValueAsNumber(key: String) = null

                    override fun recordExceptionImpl(throwable: Throwable) {

                    }

                    override fun recordFeedback(rating: Int, email: String?, comment: String) {

                    }
                }
            }
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
                BluetoothManager()
            }
            single {
                DeviceManager(
                    get(),
                    get(),
                    get(),
                    emptyList()
                )
            }
            single<FcmCommon> {
                object : FcmCommon(get()) {
                    override fun showDebugNotification(title: String, message: String) {

                    }

                    override fun showBuyTransactionNotification(
                        meldNotificationData: MeldNotificationData
                    ) {
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

package com.blockstream.compose.di

import com.blockstream.common.CountlyBase
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.NoKeystore
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.di.initKoin
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.fcm.Firebase
import com.blockstream.common.lightning.BreezNotification
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.DeviceManager
import org.koin.dsl.module

fun initKoinDesktop(appConfig: AppConfig, appInfo: AppInfo, doOnStartup: () -> Unit = {}) {

    initKoin(
        appInfo = appInfo,
        appConfig = appConfig,
        doOnStartup = doOnStartup,
        module {
            single<CountlyBase> {
                // Dummy
                object : CountlyBase(get(), get(), get(), get()){
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
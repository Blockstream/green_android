package com.blockstream.common.di

import co.touchlab.kermit.Logger
import com.blockstream.common.CountlyBase
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.EncryptedData
import com.blockstream.common.database.DriverFactory
import com.blockstream.common.managers.AssetQATester
import com.blockstream.common.managers.LifecycleManager
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalStdlibApi::class)
fun initKoinIos(
    appConfig: AppConfig,
    appInfo: AppInfo,
    userDefaults: NSUserDefaults,
    doOnStartup: () -> Unit
): KoinApplication = initKoin(appConfig,
    module {
        single { appInfo }
        single<Settings> { NSUserDefaultsSettings(userDefaults) }
        single<AssetQATester?> { object : AssetQATester{
            override fun isAssetFetchDisabled(): Boolean = false
        } }
        single<CountlyBase> {
            // Dummy
            object : CountlyBase(get(), get(), get(), get()){
                override fun updateRemoteConfig() {}

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

                override fun recordException(throwable: Throwable) {

                }

            }
        }
        single {
            // TODO IMPLEMENT IT
            object : GreenKeystore{
                override fun encryptData(dataToEncrypt: ByteArray): EncryptedData {
                    return EncryptedData.fromByteArray(dataToEncrypt, byteArrayOf())
                }

                override fun encryptData(
                    cipher: PlatformCipher,
                    dataToEncrypt: ByteArray
                ): EncryptedData {
                    TODO("Not yet implemented")
                }

                override fun decryptData(encryptedData: EncryptedData): ByteArray {
                    return encryptedData.getEncryptedData()
                }

                override fun decryptData(
                    cipher: PlatformCipher,
                    encryptedData: EncryptedData
                ): ByteArray {
                    TODO("Not yet implemented")
                }

                override fun canUseBiometrics(): Boolean {
                    TODO("Not yet implemented")
                }

            }
        }
        single { doOnStartup }
    }
)

actual val platformModule = module {
    singleOf(::DriverFactory)
}

// Access from Swift to create a logger
@Suppress("unused")
fun Koin.loggerWithTag(tag: String) =
    get<Logger>(qualifier = null) { parametersOf(tag) }

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getLifecycleManager()= getKoin().get<LifecycleManager>()
}

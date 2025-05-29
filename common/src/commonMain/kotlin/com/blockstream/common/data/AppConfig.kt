package com.blockstream.common.data

import blockstream_green.common.generated.resources.Res
import com.blockstream.green.utils.Loggable
import kotlinx.coroutines.runBlocking
import okio.internal.commonToUtf8String

data class AppConfig(
    val isDebug: Boolean,
    val filesDir: String,
    val cacheDir: String,
    val breezApiKey: String? = null,
    val greenlightKey: String? = null,
    val greenlightCert: String? = null,
    val zendeskClientId: String? = null,
    val analyticsFeatureEnabled: Boolean = true,
    val lightningFeatureEnabled: Boolean = true,
    val storeRateEnabled: Boolean = false
) {
    companion object : Loggable() {
        fun default(
            isDebug: Boolean,
            filesDir: String,
            cacheDir: String,
            analyticsFeatureEnabled: Boolean,
            lightningFeatureEnabled: Boolean,
            storeRateEnabled: Boolean
        ): AppConfig {
            val appKeys: AppKeys? = runBlocking {
                Res.readBytes("files/app_keys.txt").commonToUtf8String().let {
                    AppKeys.fromText(it)
                }
            }

            if (lightningFeatureEnabled && (appKeys?.greenlightCert == null || appKeys.greenlightKey == null || appKeys.breezApiKey == null)) {
                logger.i { "Lightning Feature turned off" }
            }

            return AppConfig(
                isDebug = isDebug,
                filesDir = filesDir,
                cacheDir = cacheDir,
                breezApiKey = appKeys?.breezApiKey,
                greenlightKey = appKeys?.greenlightKey,
                greenlightCert = appKeys?.greenlightCert,
                zendeskClientId = appKeys?.zendeskClientId,
                analyticsFeatureEnabled = analyticsFeatureEnabled,
                lightningFeatureEnabled = lightningFeatureEnabled && appKeys?.greenlightCert != null,
                storeRateEnabled = storeRateEnabled
            )
        }
    }
}
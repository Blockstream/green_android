package com.blockstream.common.data

data class AppConfig constructor(
    val isDebug: Boolean,
    val gdkDataDir: String,
    val breezApiKey: String? = null,
    val greenlightKey: String? = null,
    val greenlightCert: String? = null,
    val zendeskClientId: String? = null,
    val analyticsFeatureEnabled: Boolean = true,
    val lightningFeatureEnabled: Boolean = true,
    val storeRateEnabled: Boolean = false
) {
    companion object {
        fun default(
            isDebug: Boolean,
            gdkDataDir: String,
            appKeys: AppKeys?,
            analyticsFeatureEnabled: Boolean,
            lightningFeatureEnabled: Boolean,
            storeRateEnabled: Boolean
        ): AppConfig {
            return AppConfig(
                isDebug = isDebug,
                gdkDataDir = gdkDataDir,
                breezApiKey = appKeys?.breezApiKey,
                greenlightKey = appKeys?.greenlightKey,
                greenlightCert = appKeys?.greenlightCert,
                zendeskClientId = appKeys?.zendeskClientId,
                analyticsFeatureEnabled = analyticsFeatureEnabled,
                lightningFeatureEnabled = lightningFeatureEnabled,
                storeRateEnabled = storeRateEnabled
            )
        }
    }
}
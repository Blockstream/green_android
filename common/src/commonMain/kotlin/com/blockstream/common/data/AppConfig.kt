package com.blockstream.common.data

data class AppConfig constructor(
    val isDebug: Boolean,
    val gdkDataDir: String,
    val breezApiKey: String?,
    val greenlightKey: String?,
    val greenlightCert: String?,
    val zendeskClientId: String?,
    val analyticsFeatureEnabled: Boolean,
    val lightningFeatureEnabled: Boolean,
    val storeRateEnabled: Boolean
) {
    companion object {
        fun default(
            isDebug: Boolean,
            gdkDataDir: String,
            appSecrets: AppSecrets?,
            analyticsFeatureEnabled: Boolean,
            lightningFeatureEnabled: Boolean,
            storeRateEnabled: Boolean
        ): AppConfig {
            return AppConfig(
                isDebug = isDebug,
                gdkDataDir = gdkDataDir,
                breezApiKey = appSecrets?.breezApiKey,
                greenlightKey = appSecrets?.greenlightKey,
                greenlightCert = appSecrets?.greenlightCert,
                zendeskClientId = appSecrets?.zendeskClientId,
                analyticsFeatureEnabled = analyticsFeatureEnabled,
                lightningFeatureEnabled = lightningFeatureEnabled,
                storeRateEnabled = storeRateEnabled
            )
        }
    }
}
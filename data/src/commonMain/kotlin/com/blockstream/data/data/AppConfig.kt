package com.blockstream.data.data

import co.touchlab.kermit.Logger

data class AppConfig(
    val isDebug: Boolean,
    val filesDir: String,
    val cacheDir: String,
    val greenlightKey: String? = null,
    val greenlightCert: String? = null,
    val analyticsFeatureEnabled: Boolean = true,
    val lightningFeatureEnabled: Boolean = true,
    val storeRateEnabled: Boolean = false
) {
    companion object {
        fun default(
            isDebug: Boolean,
            filesDir: String,
            cacheDir: String,
            analyticsFeatureEnabled: Boolean,
            lightningFeatureEnabled: Boolean,
            storeRateEnabled: Boolean,
            greenlightCert: String?,
            greenlightKey: String?
        ): AppConfig {
            val glCert = greenlightCert?.takeIf { it.isNotBlank() }
            val glKey = greenlightKey?.takeIf { it.isNotBlank() }

            if (lightningFeatureEnabled && (glCert == null || glKey == null)) {
                Logger.i { "Lightning Feature turned off" }
            }

            return AppConfig(
                isDebug = isDebug,
                filesDir = filesDir,
                cacheDir = cacheDir,
                greenlightKey = glKey,
                greenlightCert = glCert,
                analyticsFeatureEnabled = analyticsFeatureEnabled,
                lightningFeatureEnabled = lightningFeatureEnabled && glCert != null && glKey != null,
                storeRateEnabled = storeRateEnabled
            )
        }
    }
}
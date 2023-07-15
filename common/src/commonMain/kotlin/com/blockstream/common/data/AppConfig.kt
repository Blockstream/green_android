package com.blockstream.common.data

data class AppConfig constructor(
    val isDebug: Boolean,
    val gdkDataDir: String,
    val greenlightApiKey: String?,
    val greenlightKey: String?,
    val greenlightCert: String?,

    val analyticsFeatureEnabled: Boolean,
    val lightningFeatureEnabled: Boolean,
    val storeRateEnabled: Boolean
)
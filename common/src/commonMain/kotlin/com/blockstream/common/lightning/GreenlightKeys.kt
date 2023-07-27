package com.blockstream.common.lightning

import breez_sdk.GreenlightCredentials


data class GreenlightKeys(
    val apiKey: String,
    val deviceKey: List<UByte>?,
    val deviceCert: List<UByte>?
) {
    fun toGreenlightCredentials(): GreenlightCredentials? {
        return if (deviceKey != null && deviceCert != null) {
            GreenlightCredentials(
                deviceKey = deviceKey,
                deviceCert = deviceCert,
            )
        } else null
    }
}
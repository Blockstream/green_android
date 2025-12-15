package com.blockstream.data.lightning

import breez_sdk.GreenlightCredentials

data class GreenlightKeys(
    val breezApiKey: String,
    val deviceKey: List<UByte>?,
    val deviceCert: List<UByte>?
) {
    fun toGreenlightCredentials(): GreenlightCredentials? {
        return if (deviceKey != null && deviceCert != null) {
            GreenlightCredentials(
                developerKey = deviceKey,
                developerCert = deviceCert,
            )
        } else null
    }
}
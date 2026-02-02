@file:OptIn(ExperimentalUnsignedTypes::class)

package com.blockstream.data.lightning

import breez_sdk.GreenlightCredentials
import com.blockstream.glsdk.DeveloperCert

class GreenlightKeys(
    val breezApiKey: String,
    val deviceKey: ByteArray?,
    val deviceCert: ByteArray?
) {
    val developerCert: DeveloperCert?
        get() = if (deviceKey != null && deviceCert != null) {
            DeveloperCert(cert = deviceCert, key = deviceKey)
        } else null

    fun toGreenlightCredentials(): GreenlightCredentials? {
        return if (deviceKey != null && deviceCert != null) {
            GreenlightCredentials(
                developerKey = deviceKey.toUByteArray().toTypedArray()
                    .toList(),
                developerCert = deviceCert.toUByteArray().toTypedArray()
                    .toList(),
            )
        } else null
    }
}
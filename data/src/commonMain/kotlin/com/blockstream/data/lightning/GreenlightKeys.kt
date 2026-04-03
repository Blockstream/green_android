@file:OptIn(ExperimentalUnsignedTypes::class)

package com.blockstream.data.lightning

import com.blockstream.glsdk.DeveloperCert

class GreenlightKeys(
    val deviceKey: ByteArray?,
    val deviceCert: ByteArray?
) {
    val developerCert: DeveloperCert?
        get() = if (deviceKey != null && deviceCert != null) {
            DeveloperCert(cert = deviceCert, key = deviceKey)
        } else null
}
package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncryptWithPin constructor(
    var networkInjected: Network? = null,
    @SerialName("pin_data") val pinData: PinData,
): GAJson<EncryptWithPin>() {
    override fun kSerializer() = serializer()

    val network
        get() = networkInjected!!
}
package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncryptWithPin constructor(
    var networkInjected: Network? = null,
    @SerialName("pin_data") val pinData: PinData,
): GdkJson<EncryptWithPin>() {
    override fun kSerializer() = serializer()

    val network
        get() = networkInjected!!
}
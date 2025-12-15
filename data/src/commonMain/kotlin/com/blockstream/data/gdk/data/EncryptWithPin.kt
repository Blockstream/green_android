package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncryptWithPin constructor(
    var networkInjected: Network? = null,
    @SerialName("pin_data")
    val pinData: PinData,
) : GreenJson<EncryptWithPin>() {
    override fun kSerializer() = serializer()

    val network
        get() = networkInjected!!
}
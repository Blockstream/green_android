package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
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
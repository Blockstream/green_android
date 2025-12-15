package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiveAddressParams(
    @SerialName("subaccount")
    val subaccount: Long,
    @SerialName("ignore_gap_limit")
    val ignoreGapLimit: Boolean = false,
) : GreenJson<ReceiveAddressParams>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
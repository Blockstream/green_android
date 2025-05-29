package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreviousAddressParams constructor(
    @SerialName("subaccount")
    val subaccount: Long,
    @SerialName("last_pointer")
    val lastPointer: Int? = null,
) : GreenJson<PreviousAddressParams>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}

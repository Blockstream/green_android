package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
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

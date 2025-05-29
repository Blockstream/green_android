package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreviousAddresses(
    @SerialName("last_pointer")
    val lastPointer: Int? = null,
    @SerialName("list")
    val addresses: List<Address> = listOf(),
) : GreenJson<PreviousAddresses>() {

    override fun kSerializer() = serializer()
}
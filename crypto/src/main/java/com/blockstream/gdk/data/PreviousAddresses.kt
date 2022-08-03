package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PreviousAddresses(
    @SerialName("last_pointer") val lastPointer: Int? = null,
    @SerialName("list") val addresses: List<Address> = listOf(),
): GAJson<PreviousAddresses>(){

    override fun kSerializer() = serializer()
}
package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateSubAccountParams constructor(
    @SerialName("subaccount")
    val subaccount: Long,
    @SerialName("name")
    val name: String? = null,
    @SerialName("hidden")
    val hidden: Boolean? = null,
) : GreenJson<UpdateSubAccountParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
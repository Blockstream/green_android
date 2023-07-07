package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UpdateSubAccountParams constructor(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("name") val name: String? = null,
    @SerialName("hidden") val hidden: Boolean? = null,
) : GdkJson<UpdateSubAccountParams>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
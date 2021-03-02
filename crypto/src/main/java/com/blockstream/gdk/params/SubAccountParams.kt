package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SubAccountParams(
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
) : GAJson<SubAccountParams>() {

    override fun kSerializer(): KSerializer<SubAccountParams> {
        return serializer()
    }
}
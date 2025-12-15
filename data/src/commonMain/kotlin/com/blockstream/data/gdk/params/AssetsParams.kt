package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetsParams(
    @SerialName("assets")
    val assets: Boolean,
    @SerialName("icons")
    val icons: Boolean,
    @SerialName("refresh")
    val refresh: Boolean,
) : GreenJson<AssetsParams>() {
    override fun kSerializer() = serializer()
}
package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AssetsParams(
    @SerialName("assets") val assets: Boolean,
    @SerialName("icons") val icons: Boolean,
    @SerialName("refresh") val refresh: Boolean,
) : GAJson<AssetsParams>() {

    override fun kSerializer(): KSerializer<AssetsParams> {
        return serializer()
    }
}

package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class GetAssetsParams constructor(
    @SerialName("assets_id") val assets: List<String>,
) : GAJson<GetAssetsParams>() {
    override fun kSerializer() = serializer()
}
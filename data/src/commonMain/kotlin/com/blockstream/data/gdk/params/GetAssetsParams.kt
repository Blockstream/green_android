package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetAssetsParams constructor(
    @SerialName("assets_id")
    val assets: List<String>,
) : GreenJson<GetAssetsParams>() {
    override fun kSerializer() = serializer()
}
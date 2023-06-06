package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class GetAssetsParams constructor(
    @SerialName("assets_id") val assets: List<String>,
) : GdkJson<GetAssetsParams>() {
    override fun kSerializer() = serializer()
}
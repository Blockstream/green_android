package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BcurEncodedData(
    @SerialName("parts")
    val parts: List<String>,
) : GreenJson<BcurEncodedData>() {
    override fun kSerializer() = serializer()
}
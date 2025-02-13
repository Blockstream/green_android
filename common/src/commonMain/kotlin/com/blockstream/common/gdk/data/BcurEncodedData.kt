package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BcurEncodedData(
    @SerialName("parts") val parts: List<String>,
): GreenJson<BcurEncodedData>(){
    override fun kSerializer() = serializer()
}
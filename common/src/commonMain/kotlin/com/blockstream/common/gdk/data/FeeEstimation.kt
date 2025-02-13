package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeeEstimation(
    @SerialName("fees") val fees: List<Long>
) : GreenJson<FeeEstimation>() {
    override fun kSerializer() = serializer()
}
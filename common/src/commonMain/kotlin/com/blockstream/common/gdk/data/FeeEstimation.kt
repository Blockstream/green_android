package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class FeeEstimation(
    @SerialName("fees") val fees: List<Long>
) : GreenJson<FeeEstimation>(), Parcelable {
    override fun kSerializer() = serializer()
}
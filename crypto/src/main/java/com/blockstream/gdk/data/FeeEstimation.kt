package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class FeeEstimation(
    @SerialName("fees") val fees: List<Long>
) : Parcelable{
    val minimumRelayFee = fees.getOrNull(0)
}
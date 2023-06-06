package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class FeeEstimation(
    @SerialName("fees") val fees: List<Long>
) : Parcelable {

    @IgnoredOnParcel
    val minimumRelayFee = fees.getOrNull(0)
}
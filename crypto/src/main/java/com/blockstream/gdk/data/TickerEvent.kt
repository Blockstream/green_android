package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TickerEvent(
    @SerialName("exchange") val exchange: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("rate") val rate: String? = null,
)
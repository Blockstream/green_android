package com.blockstream.data.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TickerEvent(
    @SerialName("type")
    val type: String? = null,
    @SerialName("exchange")
    val exchange: String? = null,
    @SerialName("currency")
    val currency: String? = null,
)
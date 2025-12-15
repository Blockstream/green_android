package com.blockstream.data.meld.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuyDefaultValues(
    @SerialName("buy_default_values")
    val buyDefaultValues: Map<String, List<String>>,
)
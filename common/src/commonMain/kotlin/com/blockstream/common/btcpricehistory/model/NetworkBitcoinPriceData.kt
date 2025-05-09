package com.blockstream.common.btcpricehistory.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkBitcoinPriceData(
    @SerialName("currency") val currency: String,
    @SerialName("last_refresh") val lastRefresh: String = "",
) {

    @SerialName("prices_day")
    val dailyPrices: List<List<Double?>> = emptyList()
        get() = field.sortedBy { it[0] }

    @SerialName("prices_full")
    val fullPrices: List<List<Double?>> = emptyList()
        get() = field.sortedBy { it[0] }

    @SerialName("prices_month")
    val monthlyPrices: List<List<Double?>> = emptyList()
        get() = field.sortedBy { it[0] }
}


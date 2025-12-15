package com.blockstream.data.btcpricehistory.model

data class BitcoinChartData(
    val currency: String,
    val currentPrice: Float,
    val lastRefreshed: Long,
    val prices: Map<BitcoinChartPeriod, List<Pair<Long, Float>>>,
)


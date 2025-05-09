package com.blockstream.common.btcpricehistory.mapper

import com.blockstream.common.btcpricehistory.model.BitcoinChartData
import com.blockstream.common.btcpricehistory.model.BitcoinChartPeriod
import com.blockstream.common.btcpricehistory.model.NetworkBitcoinPriceData
import com.blockstream.common.btcpricehistory.model.timeAgoInMillis
import kotlinx.datetime.Clock

fun NetworkBitcoinPriceData.asChartData(): BitcoinChartData {
    val data = this
    val prices = mutableMapOf<BitcoinChartPeriod, List<Pair<Long, Float>>>() //timestamp, price

    val dailyPrices = data.dailyPrices.mapAndSortNotNullPrices()
    val monthlyPrices = data.monthlyPrices.mapAndSortNotNullPrices()
    val fullPrices = data.fullPrices.mapAndSortNotNullPrices()


    prices[BitcoinChartPeriod.ONE_DAY] = dailyPrices
    prices[BitcoinChartPeriod.ONE_WEEK] = monthlyPrices.filter { it.first >= BitcoinChartPeriod.ONE_WEEK.timeAgoInMillis() }
    prices[BitcoinChartPeriod.ONE_MONTH] = monthlyPrices.filter { it.first >= BitcoinChartPeriod.ONE_MONTH.timeAgoInMillis() }
    prices[BitcoinChartPeriod.ONE_YEAR] = fullPrices.filter { it.first >= BitcoinChartPeriod.ONE_YEAR.timeAgoInMillis() }
    prices[BitcoinChartPeriod.FIVE_YEAR] = fullPrices.filter { it.first >= BitcoinChartPeriod.FIVE_YEAR.timeAgoInMillis() }

    val currentPrice = dailyPrices.maxByOrNull { it.second }?.second ?: 0f
    val lastRefreshedAt = Clock.System.now().toEpochMilliseconds()

    return BitcoinChartData(
        prices = prices.toMap(),
        currency = data.currency,
        currentPrice = currentPrice,
        lastRefreshed = lastRefreshedAt,
    )

}

private fun List<List<Double?>>.mapAndSortNotNullPrices(): List<Pair<Long, Float>> {
    return this.mapNotNull {
        val t0 = it.getOrNull(0)
        val t1 = it.getOrNull(1)
        if (t0 != null && t1 != null) {
            Pair(t0.toLong(), t1.toFloat())
        } else {
            null
        }
    }.sortedBy { it.first }
}
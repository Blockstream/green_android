package com.blockstream.data.btcpricehistory.model

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

enum class BitcoinChartPeriod(val label: String, val days: Int) {
    ONE_DAY("1D", 1),
    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    ONE_YEAR("1Y", 365),
    FIVE_YEAR("5Y", 1825)
}

fun BitcoinChartPeriod.timeAgoInMillis(): Long {
    return (Clock.System.now() - this.days.days).toEpochMilliseconds()
}
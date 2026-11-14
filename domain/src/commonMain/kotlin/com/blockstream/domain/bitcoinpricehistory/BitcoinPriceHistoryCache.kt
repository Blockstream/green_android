package com.blockstream.domain.bitcoinpricehistory

import com.blockstream.data.btcpricehistory.model.BitcoinChartData
import com.blockstream.data.data.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Process-wide cache of successfully fetched Bitcoin price history, keyed by currency, so that
 * screens observing the same currency share one result instead of each fetching and holding their
 * own copy. Failures are deliberately not stored here: they belong to the screen that triggered the
 * request, so one screen's failed refresh cannot replace another screen's working chart.
 */
class BitcoinPriceHistoryCache {

    private val prices = MutableStateFlow<Map<String, DataState<BitcoinChartData>>>(emptyMap())

    /** Emits the cached entry for [currency], or null while nothing has been cached for it yet. */
    fun observe(currency: String): Flow<DataState<BitcoinChartData>?> {
        val key = key(currency)
        return prices.map { it[key] }.distinctUntilChanged()
    }

    fun set(currency: String, data: DataState<BitcoinChartData>) {
        val key = key(currency)
        prices.update { it + (key to data) }
    }

    private fun key(currency: String) = currency.uppercase()
}

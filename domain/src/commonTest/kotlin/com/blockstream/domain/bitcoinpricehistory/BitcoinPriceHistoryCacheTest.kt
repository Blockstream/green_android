package com.blockstream.domain.bitcoinpricehistory

import com.blockstream.data.btcpricehistory.model.BitcoinChartData
import com.blockstream.data.data.DataState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BitcoinPriceHistoryCacheTest {

    private fun chartData(currency: String, price: Float): DataState<BitcoinChartData> =
        DataState.Success(
            BitcoinChartData(
                currency = currency,
                currentPrice = price,
                lastRefreshed = 0L,
                prices = emptyMap()
            )
        )

    @Test
    fun entryIsNullUntilSomethingIsCachedForThatCurrency() = runTest {
        val cache = BitcoinPriceHistoryCache()

        assertNull(cache.observe("USD").first())
    }

    @Test
    fun cachedEntryIsAvailableToAnObserverThatSubscribesLater() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val data = chartData(currency = "usd", price = 2f)

        cache.set(currency = "USD", data = data)

        assertEquals(data, cache.observe("USD").first())
    }

    @Test
    fun twoConcurrentObserversOfTheSameCurrencyBothReceiveASingleWrite() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val data = chartData(currency = "usd", price = 3f)

        val first = mutableListOf<DataState<BitcoinChartData>>()
        val second = mutableListOf<DataState<BitcoinChartData>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.observe("USD").filterNotNull().toList(first)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.observe("USD").filterNotNull().toList(second)
        }

        cache.set(currency = "USD", data = data)

        assertEquals(listOf(data), first)
        assertEquals(listOf(data), second)
    }

    @Test
    fun currenciesAreCachedIndependently() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val usd = chartData(currency = "usd", price = 4f)
        val chf = chartData(currency = "chf", price = 5f)

        cache.set(currency = "USD", data = usd)
        cache.set(currency = "CHF", data = chf)

        assertEquals(usd, cache.observe("USD").first())
        assertEquals(chf, cache.observe("CHF").first())
    }

    @Test
    fun currencyKeyIsCaseInsensitive() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val data = chartData(currency = "usd", price = 6f)

        cache.set(currency = "usd", data = data)

        assertEquals(data, cache.observe("USD").first())
    }

    @Test
    fun observerIsNotNotifiedWhenAnotherCurrencyChanges() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val firstUsd = chartData(currency = "usd", price = 7f)
        val secondUsd = chartData(currency = "usd", price = 8f)

        val emissions = mutableListOf<DataState<BitcoinChartData>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.observe("USD").filterNotNull().toList(emissions)
        }

        cache.set(currency = "USD", data = firstUsd)
        cache.set(currency = "CHF", data = chartData(currency = "chf", price = 99f))
        cache.set(currency = "USD", data = secondUsd)

        assertEquals(listOf(firstUsd, secondUsd), emissions)
    }

    @Test
    fun writingAnEqualValueDoesNotEmitAgain() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val data = chartData(currency = "usd", price = 9f)

        val emissions = mutableListOf<DataState<BitcoinChartData>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.observe("USD").filterNotNull().toList(emissions)
        }

        cache.set(currency = "USD", data = data)
        cache.set(currency = "USD", data = data)

        assertEquals(listOf(data), emissions)
    }

    @Test
    fun concurrentWritesForDistinctCurrenciesAreAllRetained() = runTest {
        val cache = BitcoinPriceHistoryCache()
        val currencies = (0 until 50).map { "C$it" }

        withContext(Dispatchers.Default) {
            currencies.mapIndexed { index, currency ->
                async {
                    cache.set(
                        currency = currency,
                        data = chartData(currency = currency, price = index.toFloat())
                    )
                }
            }.awaitAll()
        }

        currencies.forEachIndexed { index, currency ->
            assertEquals(
                chartData(currency = currency, price = index.toFloat()),
                cache.observe(currency).first()
            )
        }
    }
}

package com.blockstream.data.btcpricehistory.mapper

import com.blockstream.data.btcpricehistory.model.BitcoinChartPeriod
import com.blockstream.data.btcpricehistory.model.NetworkBitcoinPriceData
import com.blockstream.data.data.DataState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

class BitcoinChartPriceMapperTest {
    @Test
    fun missingOrUnusableDailyPricesProduceAnEmptyState() {
        val responses = listOf(
            """{"currency":"USD"}""",
            """{"currency":"USD","prices_day":[]}""",
            """{"currency":"USD","prices_day":[[],[1000],[null,10],[1000,null]]}""",
            """{"currency":"USD","prices_day":[],"prices_month":[[1000,10]],"prices_full":[[1000,10]]}""",
        )
        for (response in responses) {
            val chart = Json.decodeFromString<NetworkBitcoinPriceData>(response).asChartData()
            assertNull(chart, response)
            assertEquals(DataState.Empty, DataState.successOrEmpty(chart))
        }
    }

    @Test
    fun malformedRowsAreSkippedInEveryPriceSeries() {
        val now = Clock.System.now().toEpochMilliseconds()
        val earlier = now - 1000
        val rows = "[[],[$now,20],[$earlier],[$earlier,10],[null,30],[$now,null]]"
        val response = """{"currency":"USD","prices_day":$rows,"prices_month":$rows,"prices_full":$rows}"""

        val chart = assertNotNull(Json.decodeFromString<NetworkBitcoinPriceData>(response).asChartData())

        assertEquals("USD", chart.currency)
        assertEquals(20f, chart.currentPrice)
        for (period in BitcoinChartPeriod.entries) {
            assertEquals(listOf(earlier to 10f, now to 20f), chart.prices[period], period.name)
        }
    }

    @Test
    fun validDailyPricesAreSortedAndTheLatestPriceIsUsed() {
        val response = """{"currency":"EUR","prices_day":[[2000,20],[1000,10]]}"""

        val chart = assertNotNull(Json.decodeFromString<NetworkBitcoinPriceData>(response).asChartData())

        assertEquals("EUR", chart.currency)
        assertEquals(20f, chart.currentPrice)
        assertEquals(listOf(1000L to 10f, 2000L to 20f), chart.prices[BitcoinChartPeriod.ONE_DAY])
        assertEquals(emptyList(), chart.prices[BitcoinChartPeriod.ONE_MONTH])
        assertEquals(emptyList(), chart.prices[BitcoinChartPeriod.FIVE_YEAR])
    }
}

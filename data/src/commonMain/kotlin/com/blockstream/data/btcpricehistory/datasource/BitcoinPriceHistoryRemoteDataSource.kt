package com.blockstream.data.btcpricehistory.datasource

import com.blockstream.data.btcpricehistory.BitcoinPriceHistoryHttpClient
import com.blockstream.data.btcpricehistory.model.NetworkBitcoinPriceData
import com.blockstream.network.NetworkResponse

class BitcoinPriceHistoryRemoteDataSource(
    private val client: BitcoinPriceHistoryHttpClient
) {
    suspend fun getPriceHistory(currency: String): NetworkResponse<NetworkBitcoinPriceData> {
        return client.get<NetworkBitcoinPriceData>(BASE_URL) {
            url {
                parameters.append("currency", currency)
            }
        }
    }

    companion object {
        const val BASE_URL = "https://green-btc-chart.blockstream.com/api/v1/bitcoin/prices"
    }
}
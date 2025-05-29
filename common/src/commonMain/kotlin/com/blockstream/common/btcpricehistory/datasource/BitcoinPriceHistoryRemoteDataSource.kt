package com.blockstream.common.btcpricehistory.datasource

import com.blockstream.common.btcpricehistory.BitcoinPriceHistoryHttpClient
import com.blockstream.common.btcpricehistory.model.NetworkBitcoinPriceData
import com.blockstream.green.network.NetworkResponse

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
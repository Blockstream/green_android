package com.blockstream.data.btcpricehistory

import com.blockstream.data.btcpricehistory.datasource.BitcoinPriceHistoryRemoteDataSource

class BitcoinPriceHistoryRepository(
    private val remoteDataSource: BitcoinPriceHistoryRemoteDataSource
) {
    suspend fun getPriceHistory(currency: String) = remoteDataSource.getPriceHistory(currency)
}
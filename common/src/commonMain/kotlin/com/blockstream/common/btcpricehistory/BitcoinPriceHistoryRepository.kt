package com.blockstream.common.btcpricehistory

import com.blockstream.common.btcpricehistory.datasource.BitcoinPriceHistoryRemoteDataSource

class BitcoinPriceHistoryRepository(
    private val remoteDataSource: BitcoinPriceHistoryRemoteDataSource
) {
    suspend fun getPriceHistory(currency: String) = remoteDataSource.getPriceHistory(currency)
}
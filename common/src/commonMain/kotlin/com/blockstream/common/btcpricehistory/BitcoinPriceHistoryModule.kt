package com.blockstream.common.btcpricehistory

import com.blockstream.common.btcpricehistory.datasource.BitcoinPriceHistoryRemoteDataSource
import org.koin.dsl.module

val btcPriceHistoryModule = module {
    single {
        BitcoinPriceHistoryHttpClient()
    }

    single {
        BitcoinPriceHistoryRemoteDataSource(get())
    }

    single {
        BitcoinPriceHistoryRepository(
            remoteDataSource = get()
        )
    }
}
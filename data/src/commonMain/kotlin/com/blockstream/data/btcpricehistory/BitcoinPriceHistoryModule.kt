package com.blockstream.data.btcpricehistory

import com.blockstream.data.btcpricehistory.datasource.BitcoinPriceHistoryRemoteDataSource
import org.koin.dsl.module

val btcPriceHistoryModule = module {
    single {
        BitcoinPriceHistoryHttpClient(get())
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
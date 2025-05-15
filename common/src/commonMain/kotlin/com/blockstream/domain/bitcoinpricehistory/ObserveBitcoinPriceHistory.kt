package com.blockstream.domain.bitcoinpricehistory

import com.blockstream.common.btcpricehistory.BitcoinPriceHistoryRepository
import com.blockstream.common.btcpricehistory.mapper.asChartData
import com.blockstream.common.btcpricehistory.model.BitcoinChartData
import com.blockstream.green.domain.base.NetworkBoundInMemoryUseCase
import com.blockstream.green.network.NetworkResponse
import kotlinx.coroutines.flow.Flow

class ObserveBitcoinPriceHistory(
    private val bitcoinPriceHistoryRepository: BitcoinPriceHistoryRepository
) : NetworkBoundInMemoryUseCase<ObserveBitcoinPriceHistory.Params, BitcoinChartData?>() {

    override suspend fun doWork(params: Params) {
        val response = bitcoinPriceHistoryRepository.getPriceHistory(params.currency)
        if (response is NetworkResponse.Success) {
            set(response.data.asChartData())
        }
    }

    override fun createObservable(params: Params): Flow<BitcoinChartData?> {
        return get()
    }

    data class Params(val currency: String) {
        companion object {
            fun create(currency: String,) = Params(currency = currency)
        }
    }
}

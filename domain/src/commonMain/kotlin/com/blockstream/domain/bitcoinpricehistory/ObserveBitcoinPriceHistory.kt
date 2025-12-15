package com.blockstream.domain.bitcoinpricehistory

import com.blockstream.data.btcpricehistory.BitcoinPriceHistoryRepository
import com.blockstream.data.btcpricehistory.mapper.asChartData
import com.blockstream.data.btcpricehistory.model.BitcoinChartData
import com.blockstream.data.data.DataState
import com.blockstream.domain.base.NetworkBoundInMemoryUseCase
import com.blockstream.network.NetworkResponse
import com.blockstream.network.exception
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class ObserveBitcoinPriceHistory(
    private val bitcoinPriceHistoryRepository: BitcoinPriceHistoryRepository
) : NetworkBoundInMemoryUseCase<ObserveBitcoinPriceHistory.Params, DataState<BitcoinChartData>>() {

    override suspend fun doWork(params: Params) {
        val response = bitcoinPriceHistoryRepository.getPriceHistory(params.currency)
        if (response is NetworkResponse.Success) {
            set(DataState.successOrEmpty(response.data.asChartData()))
        } else {
            set(DataState.Error(response.exception()))
        }
    }

    override fun createObservable(params: Params): Flow<DataState<BitcoinChartData>> {
        return get().filterNotNull()
    }

    data class Params(val currency: String) {
        companion object {
            fun create(currency: String) = Params(currency = currency)
        }
    }
}

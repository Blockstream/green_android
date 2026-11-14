package com.blockstream.domain.bitcoinpricehistory

import com.blockstream.data.btcpricehistory.BitcoinPriceHistoryRepository
import com.blockstream.data.btcpricehistory.mapper.asChartData
import com.blockstream.data.btcpricehistory.model.BitcoinChartData
import com.blockstream.data.data.DataState
import com.blockstream.domain.base.ObservableUseCase
import com.blockstream.network.NetworkResponse
import com.blockstream.network.exception
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

class ObserveBitcoinPriceHistory(
    private val bitcoinPriceHistoryRepository: BitcoinPriceHistoryRepository,
    private val cache: BitcoinPriceHistoryCache
) : ObservableUseCase<ObserveBitcoinPriceHistory.Params, DataState<BitcoinChartData>>() {

    private val error = MutableStateFlow<DataState.Error?>(null)

    override suspend fun doWork(params: Params) {
        val response = bitcoinPriceHistoryRepository.getPriceHistory(params.currency)
        if (response is NetworkResponse.Success) {
            error.value = null
            cache.set(
                currency = params.currency,
                data = DataState.successOrEmpty(response.data.asChartData())
            )
        } else {
            error.value = DataState.Error(response.exception())
        }
    }

    override fun createObservable(params: Params): Flow<DataState<BitcoinChartData>> {
        return combine(cache.observe(params.currency), error) { cached, error ->
            error ?: cached
        }.filterNotNull()
    }

    data class Params(val currency: String) {
        companion object {
            fun create(currency: String) = Params(currency = currency)
        }
    }
}

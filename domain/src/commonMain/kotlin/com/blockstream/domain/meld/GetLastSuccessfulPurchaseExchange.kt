package com.blockstream.domain.meld

import com.blockstream.domain.base.NetworkBoundInMemoryUseCase
import com.blockstream.network.NetworkResponse
import kotlinx.coroutines.flow.Flow

class GetLastSuccessfulPurchaseExchange(
    private val meldRepository: com.blockstream.data.meld.MeldRepository
) : NetworkBoundInMemoryUseCase<GetLastSuccessfulPurchaseExchange.Params, String?>() {
    override suspend fun doWork(params: Params) {
        val response = meldRepository.getTransactions(
            params.externalCustomerId,
            listOf(_root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.SETTLED)
        )

        if (response is NetworkResponse.Success && response.data.transactions.isNotEmpty()) {
            set(response.data.transactions.first().serviceProvider)
        }
    }

    override fun createObservable(params: Params): Flow<String?> {
        return get()
    }

    data class Params(
        val externalCustomerId: String
    )

}
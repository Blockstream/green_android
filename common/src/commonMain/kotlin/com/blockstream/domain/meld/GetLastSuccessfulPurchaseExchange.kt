package com.blockstream.domain.meld

import com.blockstream.green.data.meld.MeldRepository
import com.blockstream.green.data.meld.data.MeldTransactionStatus
import com.blockstream.green.domain.base.NetworkBoundInMemoryUseCase
import com.blockstream.green.network.NetworkResponse
import kotlinx.coroutines.flow.Flow

class GetLastSuccessfulPurchaseExchange(
    private val meldRepository: MeldRepository
) : NetworkBoundInMemoryUseCase<GetLastSuccessfulPurchaseExchange.Params, String?>() {
    override suspend fun doWork(params: Params) {
        val response = meldRepository.getTransactions(params.externalCustomerId, listOf(MeldTransactionStatus.SETTLED))

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
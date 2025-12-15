package com.blockstream.domain.meld

import com.blockstream.domain.base.ObservableUseCase
import com.blockstream.domain.base.Result
import com.blockstream.network.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPendingMeldTransactions(
    private val meldRepository: com.blockstream.data.meld.MeldRepository
) : ObservableUseCase<GetPendingMeldTransactions.Params, Result<List<com.blockstream.data.meld.models.MeldTransaction>>>() {

    override suspend fun doWork(params: Params) {
        val walletIds = meldRepository.getAllPendingWalletIds()
            .plus(params.externalCustomerId)
            .toSet()

        val response = meldRepository.getTransactions(
            externalCustomerId = walletIds.joinToString(","),
            statuses = listOf(_root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.SETTLING)
        )

        when (response) {
            is NetworkResponse.Success -> {
                val transactionsByWallet = response.data.transactions
                    .filter { it.externalCustomerId != null }
                    .groupBy { it.externalCustomerId!! }

                transactionsByWallet.forEach { (walletId, transactions) ->
                    meldRepository.updateTransactions(walletId, transactions)
                }

                walletIds.forEach { walletId ->
                    if (!transactionsByWallet.containsKey(walletId)) {
                        meldRepository.updateTransactions(walletId, emptyList())
                    }
                }
            }

            is NetworkResponse.Error -> {
                // logger.e { "Failed to fetch Meld transactions: ${response.message}" }
            }
        }
    }

    override fun createObservable(params: Params): Flow<Result<List<com.blockstream.data.meld.models.MeldTransaction>>> {
        return meldRepository.getTransactionFlow(params.externalCustomerId)
            .map { meldTransactions ->
                Result.Success(meldTransactions)
            }
    }

    data class Params(
        val externalCustomerId: String
    )
}
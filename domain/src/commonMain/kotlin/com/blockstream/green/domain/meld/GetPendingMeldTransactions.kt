package com.blockstream.green.domain.meld

import com.blockstream.green.data.meld.MeldRepository
import com.blockstream.green.data.meld.data.MeldTransactionStatus
import com.blockstream.green.data.meld.models.MeldTransaction
import com.blockstream.green.domain.base.ObservableUseCase
import com.blockstream.green.domain.base.Result
import com.blockstream.green.network.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPendingMeldTransactions(
    private val meldRepository: MeldRepository
) : ObservableUseCase<GetPendingMeldTransactions.Params, Result<List<MeldTransaction>>>() {

    override suspend fun doWork(params: Params) {
        val walletIds = meldRepository.getAllPendingWalletIds()
            .plus(params.externalCustomerId)
            .toSet()

        val response = meldRepository.getTransactions(
            externalCustomerId = walletIds.joinToString(","),
            statuses = listOf(MeldTransactionStatus.SETTLING)
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

    override fun createObservable(params: Params): Flow<Result<List<MeldTransaction>>> {
        return meldRepository.getTransactionFlow(params.externalCustomerId)
            .map { meldTransactions ->
                Result.Success(meldTransactions)
            }
    }

    data class Params(
        val externalCustomerId: String
    )
}
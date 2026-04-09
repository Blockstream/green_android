package com.blockstream.domain.transaction

import com.blockstream.data.data.DataState
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.domain.base.ObservableUseCase
import kotlinx.coroutines.flow.Flow

class GetWalletTransactionsUseCase : ObservableUseCase<GetWalletTransactionsUseCase.Params, DataState<List<Transaction>>>() {

    override suspend fun doWork(params: Params) {
        if (params.session.isConnected) {
            params.session.updateWalletTransactions(isReset = params.isReset, isLoadMore = params.isLoadMore).join()
        }
    }

    override fun createObservable(params: Params): Flow<DataState<List<Transaction>>> {
        return params.session.walletTransactions
    }

    data class Params(
        val session: GdkSession,
        val isReset: Boolean = false,
        val isLoadMore: Boolean = false,
    )
}

package com.blockstream.common.views

import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Transaction

data class TransactionDetailsLook(
    val transaction: Transaction,
    val transactionUtxos: List<TransactionUtxo>,
    val networkFeeLook: NetworkFeeLook,
    val transactionStatusLook: TransactionStatusLook,
) {

    val transactionId
        get() = transaction.txHash

    val memo
        get() = transaction.memo

    val spv
        get() = transaction.spv

    val canRBF
        get() = transactionStatusLook.canRBF

//    val date by lazy {
//        transaction.createdAtInstant.toLocalDateTime(TimeZone.currentSystemDefault())
//    }

    companion object {
        suspend fun create(session: GdkSession, transaction: Transaction): TransactionDetailsLook {
            return TransactionDetailsLook(
                transaction = transaction,
                transactionUtxos =  TransactionUtxo.create(session = session, transaction = transaction),
                networkFeeLook = NetworkFeeLook.create(session = session, transaction = transaction),
                transactionStatusLook = TransactionStatusLook.create(session = session, transaction = transaction),

            )
        }
    }
}
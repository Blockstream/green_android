package com.blockstream.common.looks.transaction

import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.utils.Loggable

data class TransactionListLook(val transaction: Transaction) {

    companion object: Loggable() {
        fun create(transaction: Transaction): TransactionListLook {
            return TransactionListLook(transaction = transaction)
        }
    }
}
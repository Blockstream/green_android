package com.blockstream.common.gdk.data

import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.gdk.params.CreateTransactionParams

data class PendingTransaction(
    val params: CreateTransactionParams,
    val transaction: CreateTransaction,
    val segmentation: TransactionSegmentation
)
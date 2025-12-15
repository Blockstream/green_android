package com.blockstream.data.gdk.data

import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.gdk.params.CreateTransactionParams

data class PendingTransaction constructor(
    val params: CreateTransactionParams,
    val transaction: CreateTransaction,
    val segmentation: TransactionSegmentation
)
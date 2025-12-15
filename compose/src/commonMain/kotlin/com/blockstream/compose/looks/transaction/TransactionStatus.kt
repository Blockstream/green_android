package com.blockstream.compose.looks.transaction

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Transaction

sealed interface TransactionStatus {
    val confirmations: Long

    val onProgress: Boolean
        get() = this is Unconfirmed || this is Confirmed

    companion object {
        fun create(transaction: Transaction, session: GdkSession): TransactionStatus {
            val confirmations = transaction.getConfirmations(session)
            return when {
                transaction.isRefundableSwap -> {
                    Failed()
                }

                confirmations == 0L -> {
                    Unconfirmed(confirmationsRequired = transaction.network.confirmationsRequired)
                }

                confirmations < transaction.network.confirmationsRequired -> {
                    Confirmed(confirmations = confirmations, transaction.network.confirmationsRequired)
                }

                confirmations >= transaction.network.confirmationsRequired -> {
                    Completed(confirmations = confirmations)
                }

                else -> {
                    Failed()
                }
            }
        }
    }
}

data class Unconfirmed(val confirmationsRequired: Long = 6) : TransactionStatus {
    override val confirmations: Long = 0
}

data class Confirmed(override val confirmations: Long, val confirmationsRequired: Long = 6) : TransactionStatus
data class Completed(override val confirmations: Long = Long.MAX_VALUE) : TransactionStatus
data class Failed(val error: String = "") : TransactionStatus {
    override val confirmations: Long = 0
}
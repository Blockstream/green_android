package com.blockstream.common.looks.transaction

import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Transaction


sealed interface TransactionStatus {
    val onProgress: Boolean
        get() = this is Unconfirmed || this is Confirmed

    companion object{
        fun create(transaction: Transaction, session: GdkSession): TransactionStatus {
            val confirmations = transaction.getConfirmations(session)
            return when {
                transaction.isRefundableSwap -> {
                    Failed()
                }
                confirmations == 0L -> {
                    Unconfirmed(transaction.network.confirmationsRequired)
                }

                confirmations < transaction.network.confirmationsRequired -> {
                    Confirmed(confirmations = confirmations.toInt(), transaction.network.confirmationsRequired)
                }

                confirmations >= transaction.network.confirmationsRequired -> {
                    Completed
                }

                else -> {
                    Failed()
                }
            }
        }
    }
}

class Unconfirmed(val confirmationsRequired: Int = 6) : TransactionStatus
class Confirmed(val confirmations: Int, val confirmationsRequired : Int = 6) : TransactionStatus
object Completed : TransactionStatus
class Failed(val error: String = "") : TransactionStatus
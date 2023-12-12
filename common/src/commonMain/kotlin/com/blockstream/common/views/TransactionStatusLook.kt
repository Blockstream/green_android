package com.blockstream.common.views

import com.blockstream.common.extensions.getConfirmationsMax
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Transaction

data class TransactionStatusLook(
    val confirmations: Int,
    val confirmationsRequired: Int,
    val spv: Transaction.SPVResult,
    val isPendingCloseChannel: Boolean,
    val isRefundableSwap: Boolean,
    val canRBF: Boolean,
) {

    val statusText
        get() = when {
            isRefundableSwap -> {
                "id_failed"
            }

            confirmations == 0 -> {
                "id_unconfirmed"
            }

            confirmations < confirmationsRequired || isPendingCloseChannel -> {
                "id_pending_confirmation"
            }

            spv.inProgressOrUnconfirmed() -> {
                "id_verifying_transactions"
            }

            spv == Transaction.SPVResult.NotVerified -> {
                "id_invalid_merkle_proof"
            }

            spv == Transaction.SPVResult.NotLongest -> {
                "id_not_on_longest_chain"
            }

            spv == Transaction.SPVResult.Verified -> {
                "id_verified"
            }

            else -> {
                "id_completed"
            }
        }

    val statusColor
        get() = if (confirmations < confirmationsRequired || spv.inProgressOrUnconfirmed()) {
            Color.LOW
        } else if (isRefundableSwap || spv == Transaction.SPVResult.NotVerified) {
            Color.RED
        } else if (spv == Transaction.SPVResult.NotLongest) {
            Color.ORANGE
        } else {
            Color.GREEN
        }

    companion object {
        fun create(session: GdkSession, transaction: Transaction): TransactionStatusLook {

            return TransactionStatusLook(
                confirmations = transaction.getConfirmationsMax(session),
                confirmationsRequired = transaction.network.confirmationsRequired,
                spv = transaction.spv,
                isPendingCloseChannel = transaction.isPendingCloseChannel,
                isRefundableSwap = transaction.isRefundableSwap,
                canRBF = transaction.canRBF && !transaction.isIn && !session.isWatchOnly
            )
        }
    }
}
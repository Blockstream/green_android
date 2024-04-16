package com.blockstream.common.looks.transaction

import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.toAmountLookOrNa

data class TransactionLook(
    val status: TransactionStatus,
    val confirmations: Long,
    val transaction: Transaction,
    val assets: List<String>
) {

    val asMasked: TransactionLook
        get() = copy(assets = assets.map { "*****" })

    val directionText: String
        get() = when {
            transaction.txType == Transaction.Type.IN -> {
                if (confirmations > 0) "id_received" else "id_receiving"
            }

            transaction.txType == Transaction.Type.OUT -> {
                if (confirmations > 0) "id_sent" else "id_sending"
            }

            transaction.txType == Transaction.Type.REDEPOSIT -> "id_redeposited"
            transaction.txType == Transaction.Type.MIXED -> "id_swap"

            else -> "id_unknown"
        }

    companion object : Loggable() {
        suspend fun create(transaction: Transaction, session: GdkSession): TransactionLook {

            return TransactionLook(
                transaction = transaction,
                confirmations = transaction.getConfirmations(session),
                status = TransactionStatus.create(transaction, session),
                assets = transaction.assets.map {
                    session.starsOrNull ?: it.second.toAmountLookOrNa(
                        session = session,
                        assetId = it.first,
                        withUnit = true,
                        withDirection = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                }
            )
        }
    }
}
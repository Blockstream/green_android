package com.blockstream.common.looks.transaction

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_received
import blockstream_green.common.generated.resources.id_receiving
import blockstream_green.common.generated.resources.id_redeposited
import blockstream_green.common.generated.resources.id_sending
import blockstream_green.common.generated.resources.id_sent
import blockstream_green.common.generated.resources.id_swap
import blockstream_green.common.generated.resources.id_unknown
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.green.utils.Loggable
import com.blockstream.common.utils.toAmountLookOrNa
import org.jetbrains.compose.resources.StringResource

data class TransactionLook constructor(
    val status: TransactionStatus,
    val transaction: Transaction,
    val assets: List<String>
) {

    val asMasked: TransactionLook
        get() = copy(assets = assets.map { "*****" })

    val directionText: StringResource
        get() = when {
            transaction.txType == Transaction.Type.IN -> {
                if (status.confirmations > 0) Res.string.id_received else Res.string.id_receiving
            }

            transaction.txType == Transaction.Type.OUT -> {
                if (status.confirmations > 0) Res.string.id_sent else Res.string.id_sending
            }

            transaction.txType == Transaction.Type.REDEPOSIT -> Res.string.id_redeposited
            transaction.txType == Transaction.Type.MIXED -> Res.string.id_swap

            else -> Res.string.id_unknown
        }

    companion object : Loggable() {
        suspend fun create(transaction: Transaction, session: GdkSession, disableHideAmounts: Boolean = false): TransactionLook {

            return TransactionLook(
                transaction = transaction,
                status = TransactionStatus.create(transaction, session),
                assets = transaction.assets.map {
                    session.starsOrNull.takeIf { !disableHideAmounts } ?: it.second.toAmountLookOrNa(
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
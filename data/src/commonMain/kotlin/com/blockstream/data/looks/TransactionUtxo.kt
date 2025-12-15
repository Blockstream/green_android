package com.blockstream.data.looks

import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.utils.toAmountLookOrNa

class TransactionUtxo(
    val address: String?,
    val assetId: String?,
    val amount: String,
    val amountFiat: String
) {
    companion object {

        suspend fun create(session: GdkSession, transaction: Transaction): List<TransactionUtxo> {
            return transaction.utxoViews.map {
                create(session = session, utxoView = it)
            }
        }

        suspend fun create(session: GdkSession, utxoView: UtxoView): TransactionUtxo {

            val amount = session.starsOrNull ?: utxoView.satoshi.toAmountLookOrNa(
                session = session,
                assetId = utxoView.assetId,
                withUnit = true,
                withDirection = true,
                withGrouping = true,
                withMinimumDigits = false
            )

            val amountFiat = session.starsOrNull ?: utxoView.satoshi.toAmountLookOrNa(
                session = session,
                assetId = utxoView.assetId,
                withUnit = true,
                denomination = Denomination.fiat(session),
                withGrouping = true,
            )

            return TransactionUtxo(
                address = utxoView.address,
                assetId = utxoView.assetId,
                amount = amount,
                amountFiat = amountFiat
            )
        }
    }
}
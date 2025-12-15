package com.blockstream.compose.extensions

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_address
import blockstream_green.common.generated.resources.id_amount
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.utils.toAmountLookOrNa
import com.blockstream.compose.utils.StringHolder

suspend fun Transaction.details(session: GdkSession, database: Database): List<Pair<StringHolder, StringHolder>> = extras?.map {
    StringHolder.create(it.first) to StringHolder.create(it.second)
} ?: run {
    buildList<Pair<StringHolder, StringHolder>> {
        // TODO Show swap id
        /*
        database.getSwapFromTxHash(txHash = txHash)?.also {
            add(StringHolder.create(Res.string.id_swap_id) to StringHolder(string = it.id))
            it.invoice?.also { invoice ->
                add(StringHolder.create(Res.string.id_invoice) to StringHolder(string = invoice))
            }
        }
         */

        utxoViews.takeIf { it.size > 1 }?.forEach { utxo ->
            utxo.address?.also {
                add(
                    StringHolder.create(
                        Res.string.id_address
                    ) to StringHolder.create(it)
                )
                add(
                    StringHolder.create(Res.string.id_amount) to StringHolder.create(
                        utxo.satoshi.toAmountLookOrNa(
                            session = session,
                            assetId = utxo.assetId,
                            withUnit = true,
                            withDirection = true
                        )
                    )
                )
            }
        }
    }
}
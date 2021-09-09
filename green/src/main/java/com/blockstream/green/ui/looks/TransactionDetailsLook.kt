package com.blockstream.green.ui.looks

import com.blockstream.gdk.data.Transaction
import com.blockstream.green.gdk.GreenSession

data class TransactionDetailsLook constructor(override val session: GreenSession, override val tx: Transaction): TransactionLook(session, tx) {
    override val showFiat = true
    override val assetSize = if(tx.txType == Transaction.Type.REDEPOSIT) 0 else assets.size
}

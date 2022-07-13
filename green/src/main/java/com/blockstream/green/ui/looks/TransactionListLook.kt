package com.blockstream.green.ui.looks

import com.blockstream.gdk.data.Transaction
import com.blockstream.green.gdk.GreenSession

data class TransactionListLook constructor(override val session: GreenSession, override val tx: Transaction): TransactionLook(session, tx) {
    override val substractFeeFromOutgoing = false
    override val showFiat = false
    override val assetSize = if(tx.txType == Transaction.Type.REDEPOSIT) 1 else assets.size
    override val hideSPVInAsset = false
}

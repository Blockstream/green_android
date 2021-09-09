package com.blockstream.green.ui.looks

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.data.Balance
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.Convert
import com.blockstream.green.databinding.ListItemTransactionAssetBinding
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.utils.*
import mu.KLogging

data class TransactionListLook constructor(override val session: GreenSession, override val tx: Transaction): TransactionLook(session, tx) {
    override val showFiat = false
    override val assetSize = if(tx.txType == Transaction.Type.REDEPOSIT) 1 else assets.size
}

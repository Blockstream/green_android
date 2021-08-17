package com.blockstream.green.ui.looks

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.gdk.BalancePair
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.utils.format
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.toAssetLook
import com.blockstream.green.utils.toBTCLook

class TransactionListLook(val session: GreenSession, private val tx: Transaction) {

    val assets : List<BalancePair> = tx.satoshi.toSortedMap { o1, o2 ->
        if (o1 == session.network.policyAsset) -1 else o1.compareTo(o2)
    }.mapNotNull {
        if(session.isLiquid && it.key == session.network.policyAsset && tx.txType == Transaction.Type.OUT && tx.satoshi.size > 1){
            // Remove to display fee as amount in liquid
            null
        }else {
            it.toPair()
        }
    }


    val date
        get() = tx.createdAt.format()

    val memo
        get() = tx.memo

    val assetSize
        get() = if(tx.txType == Transaction.Type.REDEPOSIT) 1 else assets.size

    fun amount(index: Int): String {
        if (tx.txType == Transaction.Type.REDEPOSIT) {
            // Fee
            return tx.fee.toBTCLook(session, withUnit = false, withDirection = tx.txType, withGrouping = true)
        }

        assets[index].let{
            return if(it.first == session.network.policyAsset){
                it.second.toBTCLook(session, withUnit = false, withDirection = tx.txType, withGrouping = true)
            }else{
                val assetId = it.first
                it.second.toAssetLook(
                    session,
                    assetId,
                    withUnit = false,
                    withDirection = tx.txType,
                    withGrouping = true
                )
            }
        }
    }

    val valueColor
        get() = if (tx.isIn) R.color.brand_green else R.color.white

    fun ticker(index: Int): String {
        return assets[index].let{
            if (it.first == session.network.policyAsset) {
                getBitcoinOrLiquidUnit(session)
            } else {
                val assetId = it.first
                session.getAsset(assetId)?.ticker ?: "N/A"
            }
        }
    }

    fun getIcon(index: Int, context: Context): Drawable? {
        return if (session.isLiquid) {
            assets[index].let{
                session.getAsset(it.first)?.getIcon(context, session)
            }
        } else if (session.isMainnet) {
            ContextCompat.getDrawable(context, R.drawable.ic_bitcoin_network_60)
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_bitcoin_testnet_network_60)
        }
    }
}

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
import com.greenaddress.greenapi.model.Conversion
import mu.KLogging

abstract class TransactionLook constructor(open val session: GreenSession, internal open val tx: Transaction) {
    abstract val showFiat : Boolean
    abstract val assetSize : Int
    abstract val hideSPVInAsset: Boolean

    val assets by lazy {
        tx.assets(session.network)
    }

    val date
        get() = tx.createdAt.formatAuto()

    val memo: String
        get() = tx.memo
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")

    val fee : String
        get() = tx.fee.toBTCLook(session, withUnit = true, withGrouping = true, withMinimumDigits = true)

    val feeFiat: String?
        get() = session.convertAmount(Convert(satoshi = tx.fee)).fiatOrNull(session, withUnit = true).let {
            if(it == null) it else "≈ $it"
        }

    val feeRate : String
        get() = tx.feeRate.toFeeRate()

    fun isPolicyAsset(index: Int) = assets[index].first == session.policyAsset


    fun amount(index: Int): String {
        if (tx.txType == Transaction.Type.REDEPOSIT) {
            return tx.fee.toBTCLook(session, withUnit = false, withDirection = tx.txType, withGrouping = true, withMinimumDigits = true)
        }

        assets[index].let{
            return if(it.first == session.network.policyAsset){
                it.second.toBTCLook(session, withUnit = false, withDirection = tx.txType, withGrouping = true, withMinimumDigits = true)
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


    fun fiat(index: Int): String? {
        assets[index].let{
            return if(it.first == session.network.policyAsset){
                session.convertAmount(Convert(satoshi = it.second)).fiat(session, withUnit = true)
            }else{
                null
            }
        }
    }

    private val valueColor
        get() = if (tx.isIn) R.color.brand_green else R.color.white

    fun ticker(index: Int): String {
        return assets[index].let{
            if (it.first == session.network.policyAsset) {
                getBitcoinOrLiquidUnit(session)
            } else {
                val assetId = it.first
                session.getAsset(assetId)?.ticker ?: "n/a"
            }
        }
    }

    fun getIcon(index: Int, context: Context): Drawable? {
        return when {
            session.isLiquid -> {
                assets[index].first.getAssetIcon(context, session)
            }
            session.isMainnet -> {
                ContextCompat.getDrawable(context, R.drawable.ic_bitcoin_network_60)
            }
            else -> {
                ContextCompat.getDrawable(context, R.drawable.ic_bitcoin_testnet_network_60)
            }
        }
    }

    fun setAssetToBinding(index: Int, binding: ListItemTransactionAssetBinding) {
        binding.directionColor = ContextCompat.getColor(binding.root.context, valueColor)

        binding.amount = amount(index)
        binding.fiat = if(showFiat) fiat(index)?.let { "≈ $it" } else null

        binding.ticker.text = ticker(index)
        binding.icon.setImageDrawable(getIcon(index, binding.icon.context))

        if (hideSPVInAsset || tx.spv.disabledOrVerified()) {
            binding.spv.isVisible = false
        } else {
            binding.spv.isVisible = true
            binding.spv.setImageResource(
                when (tx.spv) {
                    Transaction.SPVResult.InProgress -> R.drawable.ic_spv_in_progress
                    Transaction.SPVResult.NotLongest -> R.drawable.ic_spv_warning
                    else -> R.drawable.ic_spv_error
                }
            )
        }
    }

    companion object: KLogging()
}

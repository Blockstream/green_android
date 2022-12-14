package com.blockstream.green.ui.looks

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.Convert
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionAssetBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.utils.*
import mu.KLogging
import kotlin.math.absoluteValue

abstract class TransactionLook constructor(open val session: GreenSession, internal open val tx: Transaction): FeeLookInterface, AddreseeLookInterface {
    abstract val substractFeeFromOutgoing : Boolean
    abstract val showFiat : Boolean
    abstract val assetSize : Int
    abstract val hideSPVInAsset: Boolean

    val assets by lazy {
        tx.assets(session.network)
    }

    val date by lazy { tx.createdAt.formatAuto() }

    val memo: String by lazy {
        tx.memo
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")
    }

    override fun isChange(index: Int): Boolean = false

    override val txType
        get() = tx.txType

    override val fee : String
        get() = tx.fee.toAmountLookOrNa(session, withUnit = true, withGrouping = true, withMinimumDigits = true)

    override val feeFiat: String?
        get() = session.convertAmount(Convert(satoshi = tx.fee))
            ?.toAmountLook(session = session, isFiat = true, withUnit = true)?.let {
                "≈ $it"
            }

    override val feeRate : String
        get() = tx.feeRate.feeRateWithUnit()

    override fun getAssetId(index: Int): String = assets[index].first

    // GDK returns non-confidential addresses for Liquid. Hide them for now
    override fun getAddress(index: Int): String? = when {
        session.isLiquid -> null
        txType == Transaction.Type.OUT -> tx.outputs.filter { !it.address.isNullOrBlank() && it.isRelevant == false }.getOrNull(index)?.address
        txType == Transaction.Type.IN -> tx.outputs.filter { !it.address.isNullOrBlank() && it.isRelevant == true }.getOrNull(index)?.address
        else -> null
    }

    override fun getAmount(index: Int): Long {
        return getAmountAndAsset(index).second
    }

    private fun getAmountAndAsset(index: Int): Pair<String,Long> {
        return (if (tx.txType == Transaction.Type.REDEPOSIT) {
            session.policyAsset to -tx.fee
        } else if (substractFeeFromOutgoing && tx.txType == Transaction.Type.OUT && assets.getOrNull(index)?.first == session.policyAsset) {
            // OUT transactions in BTC/L-BTC have fee included
            assets[index].let {
                it.first to -(it.second.absoluteValue - tx.fee)
            }
        } else {
            assets.getOrNull(index)
        }) ?: (session.policyAsset to 0L)
    }

    // Cache amounts to avoid calling convert every time for performance reasons
    private val cacheAmounts = hashMapOf<Int, String>()
    fun amount(index: Int): String {
        if(!cacheAmounts.containsKey(index)) {
            val amount = getAmountAndAsset(index)
            if (tx.txType == Transaction.Type.REDEPOSIT) {
                cacheAmounts[index] = amount.second.toAmountLookOrNa(
                    session,
                    withUnit = false,
                    withDirection = true,
                    withGrouping = true,
                    withMinimumDigits = true
                )
            } else {
                cacheAmounts[index] = amount.second.toAmountLookOrNa(
                    session,
                    assetId = amount.first,
                    withUnit = false,
                    withDirection = true,
                    withGrouping = true,
                    withMinimumDigits = true
                )
            }
        }

        return cacheAmounts[index] ?: ""
    }


    fun fiat(index: Int): String? = assets[index].let {
        it.second.toAmountLook(
            session = session,
            assetId = it.first,
            isFiat = true,
            withUnit = true
        )
    }

    private fun getValueColor(index: Int): Int {
        return if (getAmount(index) < 0) R.color.white else R.color.brand_green
    }

    fun ticker(index: Int): String {
        return assets.getOrNull(index)?.let{
            if (it.first == session.network.policyAsset) {
                getBitcoinOrLiquidUnit(session)
            } else {
                val assetId = it.first
                session.getAsset(assetId)?.ticker ?: "n/a"
            }
        } ?: "-"
    }

    fun getIcon(index: Int, context: Context): Drawable? {
        return assets.getOrNull(index)?.first?.getAssetIcon(context, session)
    }

    override fun setAssetToBinding(index: Int, binding: ListItemTransactionAssetBinding) {
        binding.directionColor = ContextCompat.getColor(binding.root.context, getValueColor(index))

        binding.amount = amount(index)
        binding.fiat = if(showFiat) fiat(index)?.let { "≈ $it" } else null

        binding.ticker.text = ticker(index)
        binding.icon.setImageDrawable(getIcon(index, binding.icon.context))
        binding.icon.updateAssetPadding(session, assets.getOrNull(index)?.first ?: "-", 3)

        if (hideSPVInAsset || tx.spv.disabledOrUnconfirmedOrVerified()) {
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

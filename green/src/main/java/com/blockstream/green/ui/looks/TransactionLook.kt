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

abstract class TransactionLook constructor(open val session: GreenSession, internal open val tx: Transaction): FeeLookInterface, AddreseeLookInterface {
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
    override fun getAddress(index: Int) = if(session.isLiquid) null else tx.addressees.getOrNull(index)

    // Cache amounts to avoid calling convert every time for performance reasons
    private val cacheAmounts = hashMapOf<Int, String>()
    fun amount(index: Int): String {
        if(!cacheAmounts.containsKey(index)) {
            if (tx.txType == Transaction.Type.REDEPOSIT) {
                cacheAmounts[index] = tx.fee.toAmountLookOrNa(
                    session,
                    withUnit = false,
                    withDirection = tx.txType,
                    withGrouping = true,
                    withMinimumDigits = true
                )
            } else {
                cacheAmounts[index] = assets.getOrNull(index)?.let {
                    it.second.toAmountLookOrNa(
                        session,
                        assetId = it.first,
                        withUnit = false,
                        withDirection = tx.txType,
                        withGrouping = true,
                        withMinimumDigits = true
                    )
                } ?: "-"
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

    private val valueColor
        get() = if (tx.isIn) R.color.brand_green else R.color.white

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
        binding.directionColor = ContextCompat.getColor(binding.root.context, valueColor)

        binding.amount = amount(index)
        binding.fiat = if(showFiat) fiat(index)?.let { "≈ $it" } else null

        binding.ticker.text = ticker(index)
        binding.icon.setImageDrawable(getIcon(index, binding.icon.context))
        binding.icon.updateAssetPadding(session, assets.getOrNull(index)?.first ?: "-", 3)

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

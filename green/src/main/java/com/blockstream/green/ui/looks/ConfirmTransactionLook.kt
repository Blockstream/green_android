package com.blockstream.green.ui.looks

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.gdk.data.CreateTransaction
import com.blockstream.gdk.data.Output
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.params.Convert
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionAssetBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.utils.*

data class ConfirmTransactionLook constructor(val session: GreenSession, val tx: CreateTransaction) : FeeLookInterface, AddreseeLookInterface {

    var changeOutput: Output? = if(!tx.isSweep) tx.outputs.find { it.isChange } else null

    val recipients
        get() = tx.addressees.size

    override val fee: String
        get() = (tx.fee ?: 0).toAmountLookOrNa(
            session,
            withUnit = true,
            withGrouping = true,
            withMinimumDigits = true
        )

    override val feeFiat: String?
        get() = session.convertAmount(Convert(satoshi = tx.fee))
            ?.toAmountLook(session = session, isFiat = true, withUnit = true)?.let {
            "≈ $it"
        }

    override val feeRate: String
        get() = (tx.feeRate ?: 0).feeRateWithUnit()

    override val txType: Transaction.Type
        get() = Transaction.Type.OUT

    override fun isChange(index: Int): Boolean {
        return index >= recipients
    }

    override fun getAssetId(index: Int): String = tx.addressees.getOrNull(index)?.assetId ?: session.policyAsset // changeOutput
    override fun getAddress(index: Int): String? = tx.addressees.getOrNull(index)?.address ?: changeOutput?.address

    override fun setAssetToBinding(index: Int, binding: ListItemTransactionAssetBinding) {
        binding.directionColor = ContextCompat.getColor(binding.root.context, R.color.white)

        val assetId = tx.addressees.getOrNull(index)?.assetId ?: changeOutput?.assetId ?: session.policyAsset
        val satoshi = if(!session.isElectrum && tx.isSendAll){
            tx.satoshi[assetId]
        }else{
            tx.addressees.getOrNull(index)?.satoshi
        } ?: changeOutput?.satoshi ?: tx.satoshi[assetId] ?: 0

        binding.amount = satoshi.toAmountLook(
            session = session,
            assetId = assetId,
            withUnit = false,
            withDirection = null,
            withGrouping = true,
            withMinimumDigits = true
        )
        binding.ticker.text = if(assetId.isPolicyAsset(session)) getBitcoinOrLiquidUnit(session) else session.getAsset(assetId)?.ticker ?: "n/a"
        binding.fiat = satoshi.toAmountLook(
            session = session,
            isFiat = true,
            assetId = assetId,
        )
        binding.icon.setImageDrawable(assetId.getAssetIcon(binding.root.context, session))
        binding.icon.updateAssetPadding(session, assetId, 3)

        binding.spv.isVisible = false
    }
}
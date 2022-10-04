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

data class ConfirmTransactionLook constructor(
    val session: GreenSession,
    val tx: CreateTransaction,
    val overrideDenomination: Boolean
) : FeeLookInterface, AddreseeLookInterface {

    val changeOutputs: List<Output> by lazy {
        if (!tx.isSweep) {
            tx.outputs.filter { it.isChange }
        } else listOf()
    }

    val recipients by lazy { tx.addressees.size }

    override val fee: String
        get() = (tx.fee ?: 0).toAmountLookOrNa(
            session,
            withUnit = true,
            withGrouping = true,
            withMinimumDigits = true,
            overrideDenomination = overrideDenomination,
        )

    override val feeFiat: String?
        get() = session.convertAmount(Convert(satoshi = tx.fee))
            ?.toAmountLook(
                session = session,
                isFiat = true,
                withUnit = true,
                overrideDenomination = overrideDenomination
            )?.let {
            "≈ $it"
        }

    override val feeRate: String
        get() = (tx.feeRate ?: 0).feeRateWithUnit()

    override val txType: Transaction.Type
        get() = Transaction.Type.OUT

    override fun isChange(index: Int): Boolean {
        return index >= recipients
    }

    override fun getAssetId(index: Int): String = if(isChange(index)){
        changeOutputs.getOrNull(index - recipients)?.assetId
    }else{
        tx.addressees.getOrNull(index)?.assetId
    } ?: session.policyAsset

    override fun getAddress(index: Int): String? = tx.addressees.getOrNull(index)?.address ?: changeOutputs.getOrNull(index - recipients)?.address

    override fun getAmount(index: Int): Long = (if (isChange(index)) {
        changeOutputs.getOrNull(index - recipients)?.satoshi
    } else if (!session.isElectrum && tx.isSendAll) {
        tx.satoshi[getAssetId(index)]
    } else {
        tx.addressees.getOrNull(index)?.satoshi
    }) ?: 0L

    override fun setAssetToBinding(index: Int, binding: ListItemTransactionAssetBinding) {
        binding.directionColor = ContextCompat.getColor(binding.root.context, R.color.white)

        val assetId = getAssetId(index)
        val satoshi = getAmount(index)

        binding.amount = satoshi.toAmountLook(
            session = session,
            assetId = assetId,
            withUnit = false,
            withDirection = false,
            withGrouping = true,
            withMinimumDigits = true,
            overrideDenomination = overrideDenomination,
        )
        binding.ticker.text = if(assetId.isPolicyAsset(session)) getBitcoinOrLiquidUnit(session, BTC_UNIT.takeIf { overrideDenomination }) else session.getAsset(assetId)?.ticker ?: "n/a"
        binding.fiat = satoshi.toAmountLook(
            session = session,
            isFiat = true,
            assetId = assetId,
            overrideDenomination = overrideDenomination
        )
        binding.icon.setImageDrawable(assetId.getAssetIcon(binding.root.context, session))
        binding.icon.updateAssetPadding(session, assetId, 3)

        binding.spv.isVisible = false
    }
}
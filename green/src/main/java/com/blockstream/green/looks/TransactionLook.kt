package com.blockstream.green.looks

import androidx.core.view.isVisible
import com.blockstream.gdk.data.Transaction
import com.blockstream.gdk.data.UtxoView
import com.blockstream.gdk.params.Convert
import com.blockstream.green.R
import com.blockstream.green.databinding.TransactionAssetLayoutBinding
import com.blockstream.green.databinding.TransactionUtxoLayoutBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.getConfirmations
import com.blockstream.green.gdk.getDirectionColor
import com.blockstream.green.utils.feeRateWithUnit
import com.blockstream.green.utils.formatAuto
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toAmountLookOrNa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging

class TransactionLook constructor(val tx: Transaction, val session: GdkSession): ITransactionLook {
    val network by lazy { tx.network }
    val date by lazy { tx.createdAt.formatAuto() }

    val memo: String by lazy {
        tx.memo
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")
    }

    override suspend fun fee(): String = tx.fee.toAmountLookOrNa(
            session = session,
            assetId = tx.network.policyAsset,
            withUnit = true,
            withGrouping = true,
            withMinimumDigits = true
        )

    override suspend fun feeFiat(): String? = session.convertAmount(network, Convert(satoshi = tx.fee))
            ?.toAmountLook(session = session, isFiat = true, withUnit = true)?.let {
                "≈ $it"
            }

    override fun feeRate(): String = tx.feeRate.feeRateWithUnit()

    override val utxoSize: Int
        get() = tx.utxoViews.size

    val assetSize: Int
        get() = tx.assets.size

    override fun getUtxoView(index: Int): UtxoView? = tx.utxoViews.getOrNull(index)

    fun setTransactionAssetToBinding(
        scope: CoroutineScope,
        session: GdkSession,
        index: Int,
        binding: TransactionAssetLayoutBinding
    ) {
        tx.assets.getOrNull(index)?.also { asset ->
            binding.directionColor = asset.second.getDirectionColor(binding.root.context)

            binding.index = index
            binding.type = tx.txType
            binding.confirmations = tx.getConfirmations(session)
            binding.amount = ""
            scope.launch {
                binding.amount = session.starsOrNull ?: withContext(context = Dispatchers.IO) {
                    asset.second.toAmountLookOrNa(
                        session = session,
                        assetId = asset.first,
                        withUnit = true,
                        withDirection = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                }
            }

            if (tx.spv.disabledOrUnconfirmedOrVerified()) {
                binding.spv.isVisible = false
            } else {
                binding.spv.isVisible = true
                binding.spv.setImageResource(
                    when (tx.spv) {
                        Transaction.SPVResult.InProgress, Transaction.SPVResult.Unconfirmed -> R.drawable.ic_spv_in_progress
                        Transaction.SPVResult.NotLongest -> R.drawable.ic_spv_warning
                        else -> R.drawable.ic_spv_error
                    }
                )
            }
        }
    }

    override fun setTransactionUtxoToBinding(scope: CoroutineScope, index: Int, binding: TransactionUtxoLayoutBinding) {
        getUtxoView(index)?.also { txOutput ->
            binding.directionColor = txOutput.satoshi.getDirectionColor(binding.root.context)

            binding.amount = ""
            binding.fiat = ""

            scope.launch {
                binding.amount = session.starsOrNull ?: withContext(context = Dispatchers.IO) {
                    txOutput.satoshi.toAmountLookOrNa(
                        session = session,
                        assetId = txOutput.assetId,
                        withUnit = true,
                        withDirection = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                }

                binding.fiat = session.starsOrNull ?: withContext(context = Dispatchers.IO) {
                    txOutput.satoshi?.toAmountLook(
                        session = session,
                        assetId = txOutput.assetId,
                        isFiat = true,
                        withUnit = true
                    )?.let { "≈ $it" }
                }
            }


            binding.icon.setImageDrawable(txOutput.assetId.getAssetIcon(binding.root.context, session))
        }
    }

    companion object: KLogging()
}

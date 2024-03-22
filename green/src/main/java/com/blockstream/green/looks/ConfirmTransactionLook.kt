package com.blockstream.green.looks

import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toAmountLookOrNa
import com.blockstream.green.databinding.TransactionUtxoLayoutBinding
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.getDirectionColor
import com.blockstream.green.utils.feeRateWithUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

data class ConfirmTransactionLook constructor(
    val transaction: CreateTransaction,
    override val network: Network,
    val session: GdkSession,
    val denomination: Denomination? = null,
    val showChangeOutputs: Boolean = false,
    val isAddressVerificationOnDevice: Boolean = false,
): ITransactionLook {

    val utxos by lazy {
        transaction.utxoViews(showChangeOutputs)
    }

    override val utxoSize: Int
        get() = utxos.size

    override suspend fun fee(): String = (transaction.fee ?: 0).toAmountLookOrNa(
            session = session,
            assetId = network.policyAsset,
            withUnit = true,
            withGrouping = true,
            withMinimumDigits = true,
            denomination = if(isAddressVerificationOnDevice) Denomination.BTC else denomination
        )

    override suspend fun feeFiat(): String? = session.convert(assetId = network.policyAsset, asLong = transaction.fee)
            ?.toAmountLook(
                session = session,
                assetId = network.policyAsset,
                withUnit = true,
                denomination = Denomination.fiat(session)
            )?.let {
            "≈ $it"
        }

    private fun totalPolicyAsLong(): Long {
        return (transaction.satoshi[network.policyAsset] ?: 0).absoluteValue + (transaction.fee ?: 0)
    }
    suspend fun total(): String = totalPolicyAsLong().toAmountLookOrNa(
        session = session,
        assetId = network.policyAsset,
        withUnit = true,
        withGrouping = true,
        withMinimumDigits = true,
        denomination = if(isAddressVerificationOnDevice) Denomination.BTC else denomination
    )

    suspend fun totalFiat(): String? = session.convert(assetId = network.policyAsset, asLong = totalPolicyAsLong())
        ?.toAmountLook(
            session = session,
            assetId = network.policyAsset,
            withUnit = true,
            denomination = Denomination.fiat(session)
        )?.let {
            "≈ $it"
        }

    override fun feeRate(): String = (transaction.feeRate ?: 0).feeRateWithUnit()

    override fun getUtxoView(index: Int): UtxoView? {
        return utxos.getOrNull(index)
    }

    override fun setTransactionUtxoToBinding(scope: CoroutineScope, index: Int, binding: TransactionUtxoLayoutBinding) {
        getUtxoView(index)?.also { txOutput ->
            binding.directionColor = txOutput.satoshi.getDirectionColor(binding.root.context)

            binding.amount = ""
            binding.fiat = ""
            scope.launch {
                binding.amount = withContext(context = Dispatchers.IO) {
                    txOutput.satoshi.toAmountLookOrNa(
                        session = session,
                        assetId = txOutput.assetId,
                        withUnit = true,
                        withDirection = true,
                        withGrouping = true,
                        withMinimumDigits = false,
                        denomination = if(isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )
                }

                binding.fiat = withContext(context = Dispatchers.IO) {
                    txOutput.satoshi?.toAmountLook(
                        session = session,
                        assetId = txOutput.assetId,
                        denomination = Denomination.fiat(session),
                        withUnit = true,
                    )?.let { "≈ $it" }
                }
            }

            binding.icon.setImageDrawable(txOutput.assetId.getAssetIcon(binding.root.context, session, isLightning = network.isLightning))
        }
    }
}

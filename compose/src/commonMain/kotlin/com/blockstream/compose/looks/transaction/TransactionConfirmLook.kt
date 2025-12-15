package com.blockstream.compose.looks.transaction

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.lwk.NormalSubmarineSwap
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.toAmountLookOrNa
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue

@Serializable
data class TransactionConfirmLook(
    val from: AccountAsset? = null, val to: AccountAsset? = null,

    val isRedeposit: Boolean = false,

    val utxos: List<UtxoView>? = null,

    val amount: String? = null, val amountFiat: String? = null,

    val submarineSwap: NormalSubmarineSwap? = null,

    val serviceFee: String? = null, val serviceFeeFiat: String? = null, val serviceFeeAssetId: String? = null,

    val fee: String? = null, val feeFiat: String? = null, val feeRate: String? = null, val feeAssetId: String? = null,

    val totalFees: String? = null, val totalFeesFiat: String? = null,

    val recipientReceives: String? = null, val recipientReceivesFiat: String? = null,

    val total: String? = null, val totalFiat: String? = null
) : GreenJson<TransactionConfirmLook>() {
    override fun kSerializer() = serializer()

    companion object {
        suspend fun create(
            params: CreateTransactionParams,
            transaction: CreateTransaction,
            account: Account,
            session: GdkSession,
            denomination: Denomination? = null,
            isAddressVerificationOnDevice: Boolean = false
        ): TransactionConfirmLook {

            val isRedeposit = params.isRedeposit
            var amount: String? = null
            var amountFiat: String? = null
            var utxos: List<UtxoView>? = null

            var serviceFee: String? = null
            var serviceFeeFiat: String? = null
            var serviceFeeAssetId: String? = null

            var totalFees: String? = null
            var totalFeesFiat: String? = null
            var recipientReceives: String? = null
            var recipientReceivesFiat: String? = null

            when {
                !isAddressVerificationOnDevice && params.from != null && params.to != null -> {
                    // Find the assetId from params
                    params.addresseesAsParams?.firstOrNull()?.let {
                        val assetId = it.assetId ?: BTC_POLICY_ASSET
                        transaction.satoshi[assetId]?.absoluteValue?.let { it to assetId }
                    }?.also {
                        amount = it.first.toAmountLook(
                            session = session,
                            assetId = it.second,
                            withUnit = true,
                            withGrouping = true,
                            withMinimumDigits = isAddressVerificationOnDevice,
                            denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                        )
                        amountFiat = it.first.toAmountLook(
                            session = session,
                            assetId = it.second,
                            withUnit = true,
                            withGrouping = true,
                            denomination = Denomination.fiat(session)
                        )
                    }
                }

                params.submarineSwap != null -> {
                    val satoshi = params.submarineSwap!!.let { it.satoshi - it.serviceFee }

                    val amount = satoshi.toAmountLook(
                        session = session,
                        assetId = account.network.policyAssetOrNull,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = isAddressVerificationOnDevice,
                        denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )
                    val amountExchange = satoshi.toAmountLook(
                        session = session,
                        assetId = account.network.policyAssetOrNull,
                        withUnit = true,
                        withGrouping = true,
                        denomination = Denomination.fiat(session)
                    )

                    utxos = listOf(
                        UtxoView(
                            address = params.submarineSwap!!.bolt11Invoice,
                            assetId = account.network.policyAssetOrNull,
                            satoshi = satoshi,
                            amount = amount,
                            amountExchange = amountExchange
                        )
                    )

                    serviceFee = params.submarineSwap!!.serviceFee.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = true,
                        denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )

                    serviceFeeFiat = params.submarineSwap!!.serviceFee.takeIf { it > 0 }.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = true,
                        denomination = Denomination.fiat(session)
                    )

                    serviceFeeAssetId = account.network.policyAssetOrNull

                    val totalFeeSatoshi = params.submarineSwap!!.serviceFee + (transaction.fee ?: 0)
                    totalFees = totalFeeSatoshi.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = true,
                        denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )
                    totalFeesFiat = totalFeeSatoshi.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = true,
                        denomination = Denomination.fiat(session)
                    )

                    recipientReceives = params.submarineSwap!!.recipientReceivesSatoshi.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = true,
                        denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )
                    recipientReceivesFiat = params.submarineSwap!!.recipientReceivesSatoshi.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = true,
                        denomination = Denomination.fiat(session)
                    )
                }

                else -> {
                    utxos = transaction.utxoViews(
                        session = session,
                        denomination = denomination,
                        isAddressVerificationOnDevice = isAddressVerificationOnDevice,
                        showChangeOutputs = isAddressVerificationOnDevice && session.device?.isLedger == true
                    )
                }
            }

            val fee = transaction.fee?.toAmountLook(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = true,
                denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
            )

            val feeFiat = transaction.fee?.toAmountLook(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = true,
                denomination = Denomination.fiat(session)
            )

            val feeRate = transaction.feeRate?.feeRateWithUnit()

            val totalPolicy = (transaction.satoshi[account.network.policyAsset] ?: 0).absoluteValue + (transaction.fee ?: 0)

            val total = if (isRedeposit) fee else totalPolicy.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = true,
                denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
            )

            val totalFiat = if (isRedeposit) feeFiat else totalPolicy.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = true,
                denomination = Denomination.fiat(session)
            )

            return TransactionConfirmLook(
                from = params.from,
                to = params.to,
                isRedeposit = isRedeposit,
                amount = amount,
                amountFiat = amountFiat,
                utxos = utxos,
                submarineSwap = params.submarineSwap,
                serviceFee = serviceFee,
                serviceFeeFiat = serviceFeeFiat,
                serviceFeeAssetId = serviceFeeAssetId,
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                feeAssetId = account.network.policyAssetOrNull,
                totalFees = totalFees,
                totalFeesFiat = totalFeesFiat,
                recipientReceives = recipientReceives,
                recipientReceivesFiat = recipientReceivesFiat,
                total = total,
                totalFiat = totalFiat,
            )
        }
    }
}
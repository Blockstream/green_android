package com.blockstream.domain.send

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.toAmountLookOrNa
import kotlin.math.absoluteValue

class GetTransactionConfirmationUseCase() {

    suspend operator fun invoke(
        params: CreateTransactionParams,
        transaction: CreateTransaction,
        account: Account,
        session: GdkSession,
        denomination: Denomination? = null,
        isAddressVerificationOnDevice: Boolean = false
    ): TransactionConfirmation {

        val isRedeposit = params.isRedeposit
        var amount: String? = null
        var amountFiat: String? = null
        var utxos: List<UtxoView>? = null

        var swapFee: String? = null
        var networkFee: String? = null

        var totalFees: String? = null
        var totalFeesFiat: String? = null
        var recipientReceives: String? = null
        var recipientReceivesFiat: String? = null

        when {
            params.swap != null -> {
                val swap = requireNotNull(params.swap)

                val satoshi = swap.fromAmount

                amount = satoshi.toAmountLook(
                    session = session,
                    assetId = account.network.policyAssetOrNull,
                    withMinimumDigits = isAddressVerificationOnDevice,
                    denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                )
                amountFiat = satoshi.toAmountLook(
                    session = session,
                    assetId = account.network.policyAssetOrNull,
                    denomination = Denomination.fiat(session)
                )

                if (params.isLiquidToLightningSwap) {
                    utxos = listOf(
                        UtxoView(
                            address = swap.submarineInvoiceTo,
                            assetId = account.network.policyAssetOrNull,
                            satoshi = satoshi,
                            amount = amount,
                            amountExchange = amountFiat
                        )
                    )

                    // Show only UTXO list
                    amount = null
                    amountFiat = null
                }

                swapFee = swap.providerFee.toAmountLook(
                    session = session,
                    assetId = account.network.policyAsset,
                    withMinimumDigits = true,
                    denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                )

                networkFee = (swap.claimNetworkFee + (transaction.fee ?: 0)).toAmountLook(
                    session = session,
                    assetId = account.network.policyAsset,
                    withMinimumDigits = true,
                    denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                )

                // total fees
                (swap.providerFee + swap.claimNetworkFee + (transaction.fee ?: 0)).also {
                    totalFees = it.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withMinimumDigits = true,
                        denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )
                    totalFeesFiat = it.toAmountLook(
                        session = session,
                        assetId = account.network.policyAsset,
                        withMinimumDigits = true,
                        denomination = Denomination.fiat(session)
                    )
                }

                swap.toAmount.also {
                    recipientReceives = it.toAmountLook(
                        session = session,
                        assetId = swap.toAssetId,
                        withMinimumDigits = true,
                        denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                    )
                    recipientReceivesFiat = it.toAmountLook(
                        session = session,
                        assetId = swap.toAssetId,
                        withMinimumDigits = true,
                        denomination = Denomination.fiat(session)
                    )
                }
            }

            !isAddressVerificationOnDevice && params.isAccountTransfer && params.from != null && params.to != null -> {
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
            withMinimumDigits = true,
            denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
        )

        val feeFiat = transaction.fee?.toAmountLook(
            session = session,
            assetId = account.network.policyAsset,
            withMinimumDigits = true,
            denomination = Denomination.fiat(session)
        )

        val feeRate = transaction.feeRate?.feeRateWithUnit()

        val totalPolicy = (transaction.satoshi[account.network.policyAsset] ?: 0).absoluteValue + (transaction.fee ?: 0)

        val total = if (isRedeposit) fee else totalPolicy.toAmountLookOrNa(
            session = session,
            assetId = account.network.policyAsset,
            withMinimumDigits = true,
            denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
        )

        val totalFiat = if (isRedeposit) feeFiat else totalPolicy.toAmountLookOrNa(
            session = session,
            assetId = account.network.policyAsset,
            withMinimumDigits = true,
            denomination = Denomination.fiat(session)
        )

        return TransactionConfirmation(
            from = params.from,
            to = params.to,
            isRedeposit = isRedeposit,
            amount = amount,
            amountFiat = amountFiat,
            utxos = utxos,
            swapFee = swapFee,
            networkFee = networkFee,
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
            isLiquidToLightningSwap = params.isLiquidToLightningSwap,
            isSwap = params.isSwap,
        )
    }

}
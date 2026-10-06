package com.blockstream.domain.send

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.getFiatCurrency
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.toAmountLookOrNa
import com.blockstream.data.utils.userNumberFormat
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
        var lightningSetupFee: String? = null

        var totalFees: String? = null
        var totalFeesFiat: String? = null
        var recipientReceives: String? = null
        var recipientReceivesFiat: String? = null

        when {
            params.swap != null -> {
                val swap = requireNotNull(params.swap)

                val recipientReceiveSatoshi = swap.toAmount?.let {
                    (it - swap.lightningSetupFee).coerceAtLeast(0)
                }
                val satoshi = if (isAddressVerificationOnDevice) swap.fromAmount else recipientReceiveSatoshi
                val satoshiAssetId = if (isAddressVerificationOnDevice) swap.fromAssetId else swap.toAssetId
                val displayAssetId = if (params.isLiquidToLightningSwap) account.network.policyAsset else satoshiAssetId

                amount = satoshi.toAmountLook(
                    session = session,
                    assetId = displayAssetId,
                    withMinimumDigits = isAddressVerificationOnDevice,
                    denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                )
                amountFiat = satoshi.toAmountLook(
                    session = session,
                    assetId = displayAssetId,
                    denomination = Denomination.fiat(session)
                )

                if (params.isLiquidToLightningSwap) {
                    utxos = listOf(
                        UtxoView(
                            address = swap.submarineInvoiceTo,
                            assetId = displayAssetId,
                            satoshi = satoshi,
                            amount = amount,
                            amountExchange = amountFiat
                        )
                    )

                    // Show only UTXO list
                    amount = null
                    amountFiat = null
                }

                suspend fun Long.look(assetId: String? = account.network.policyAsset) = toAmountLook(
                    session = session,
                    assetId = assetId,
                    withMinimumDigits = true,
                    denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
                )

                suspend fun Long.lookFiat(assetId: String? = account.network.policyAsset) = toAmountLook(
                    session = session,
                    assetId = assetId,
                    withMinimumDigits = true,
                    denomination = Denomination.fiat(session)
                )

                swapFee = swap.providerFee.look()

                networkFee = (swap.claimNetworkFee + (transaction.fee ?: 0)).look()

                // Lightning channel setup fee (Bitcoin -> Lightning when a new channel is opened).
                if (swap.lightningSetupFee > 0) {
                    lightningSetupFee = swap.lightningSetupFee.look()
                }

                // total fees
                (swap.providerFee + swap.claimNetworkFee + swap.lightningSetupFee + (transaction.fee ?: 0)).also {
                    totalFees = it.look()
                    totalFeesFiat = it.lookFiat()
                }

                // Net of the Lightning setup fee: the LSP deducts the channel-open fee from the
                // received amount, so what actually lands is toAmount minus the setup fee (0 otherwise).
                val recipientReceiveDisplaySatoshi = recipientReceiveSatoshi ?: 0L
                recipientReceives = recipientReceiveDisplaySatoshi.look(swap.toAssetId)
                recipientReceivesFiat = recipientReceiveDisplaySatoshi.lookFiat(swap.toAssetId)

                if (isAddressVerificationOnDevice) {
                    utxos = transaction.utxoViews(
                        session = session,
                        denomination = denomination,
                        isAddressVerificationOnDevice = true,
                        showChangeOutputs = session.device?.isLedger == true
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

        if (isAddressVerificationOnDevice) {
            session.gdkHwWallet?.also { hwWallet ->
                utxos = utxos?.map { utxo ->
                    utxo.copy(deviceAddress = utxo.address?.let(hwWallet::displayAddress))
                }
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

        val sentAssetId = params.addresseesAsParams?.firstOrNull()?.assetId
        val isSentAssetPolicy = sentAssetId.isPolicyAsset(session)

        val total: String?
        val totalFiat: String?

        if (isRedeposit) {
            total = fee
            totalFiat = feeFiat
        } else if (isSentAssetPolicy) {
            val totalPolicy = (transaction.satoshi[account.network.policyAsset] ?: 0).absoluteValue + (transaction.fee ?: 0)


            total = totalPolicy.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                withMinimumDigits = true,
                denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
            )
            totalFiat = totalPolicy.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                withMinimumDigits = true,
                denomination = Denomination.fiat(session)
            )
        } else {
            total = null

            val sentFiatBalance = sentAssetId?.let { assetId ->
                transaction.satoshi[assetId]?.absoluteValue?.let { satoshi ->
                    session.convert(assetId = assetId, asLong = satoshi)
                }
            }
            val feeFiatBalance = transaction.fee?.let { feeAmount ->
                session.convert(assetId = account.network.policyAsset, asLong = feeAmount)
            }

            val sentFiatValue = sentFiatBalance?.fiat?.toDoubleOrNull()
            val feeFiatValue = feeFiatBalance?.fiat?.toDoubleOrNull()

            totalFiat = if (sentFiatValue != null && feeFiatValue != null) {
                userNumberFormat(
                    decimals = 2,
                    withDecimalSeparator = true,
                    withGrouping = true
                ).format(sentFiatValue + feeFiatValue)?.let { "$it ${getFiatCurrency(session)}" }
            } else {
                null
            }
        }

        return TransactionConfirmation(
            from = params.from,
            to = params.to,
            isRedeposit = isRedeposit,
            amount = amount,
            amountFiat = amountFiat,
            utxos = utxos,
            swapFee = swapFee,
            networkFee = networkFee,
            lightningSetupFee = lightningSetupFee,
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

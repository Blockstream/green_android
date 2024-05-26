package com.blockstream.common.looks.transaction

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toAmountLookOrNa
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue


@Serializable
@Parcelize
data class TransactionConfirmLook(
    val from: AccountAsset? = null,
    val to: AccountAsset? = null,

    val utxos: List<UtxoView>? = null,

    val amount: String? = null,
    val amountFiat: String? = null,

    val fee: String? = null,
    val feeFiat: String? = null,
    val feeRate: String? = null,
    val feeAssetId: String? = null,

    val total: String? = null,
    val totalFiat: String? = null
) : GreenJson<TransactionConfirmLook>(), Parcelable {
    override fun kSerializer() = serializer()

    companion object {
        suspend fun create(
            params: CreateTransactionParams,
            transaction: CreateTransaction,
            account: Account,
            session: GdkSession,
            denomination: Denomination?,
            isAddressVerificationOnDevice: Boolean = false
        ): TransactionConfirmLook {

            var amount: String? = null
            var amountFiat: String? = null
            var utxos: List<UtxoView>? = null

            if (!isAddressVerificationOnDevice && params.from != null && params.to != null) {
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
            } else {
                utxos = transaction.utxoViews(
                    session = session,
                    denomination = denomination,
                    isAddressVerificationOnDevice = isAddressVerificationOnDevice,
                    showChangeOutputs = session.device?.isLedger == true
                )
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

            val totalPolicy = (transaction.satoshi[account.network.policyAsset]
                ?: 0).absoluteValue + (transaction.fee ?: 0)

            val total = totalPolicy.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = true,
                denomination = if (isAddressVerificationOnDevice) Denomination.BTC else denomination
            )
            val totalFiat: String = totalPolicy.toAmountLookOrNa(
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
                amount = amount,
                amountFiat = amountFiat,
                utxos = utxos,
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                feeAssetId = account.network.policyAssetOrNull,
                total = total,
                totalFiat = totalFiat
            )
        }
    }
}
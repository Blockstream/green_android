package com.blockstream.data.looks

import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.toAmountLookOrNa

data class NetworkFeeLook(val fee: String, val feeFiat: String, val feeRate: String?) {

    companion object {
        suspend fun create(session: GdkSession, transaction: Transaction): NetworkFeeLook {

            val fee = transaction.fee.toAmountLookOrNa(
                session = session,
                assetId = transaction.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = true
            )

            val feeFiat = transaction.fee.toAmountLookOrNa(
                session = session,
                assetId = transaction.network.policyAsset,
                denomination = Denomination.fiat(session),
                withUnit = true,
                withGrouping = true,
            ).let {
                "≈ $it"
            }

            val feeRate = transaction.feeRate.takeIf { !transaction.account.isLightning }
                ?.let { "(${transaction.feeRate.feeRateWithUnit()})" }

            return NetworkFeeLook(fee, feeFiat, feeRate)
        }
    }
}
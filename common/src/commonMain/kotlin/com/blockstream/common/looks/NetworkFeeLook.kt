package com.blockstream.common.looks

import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.utils.feeRateKBWithUnit
import com.blockstream.common.utils.toAmountLookOrNa

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
                ?.let { "(${transaction.feeRate.feeRateKBWithUnit()})" }

            return NetworkFeeLook(fee, feeFiat, feeRate)
        }
    }
}
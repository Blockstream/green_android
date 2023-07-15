package com.blockstream.green.looks

import com.blockstream.common.gdk.data.Balance
import com.blockstream.common.gdk.params.Convert
import com.blockstream.common.data.Denomination
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.extensions.getAssetName
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.green.utils.toAmountLookOrNa
import kotlinx.coroutines.runBlocking


class AssetLook constructor(
    val assetId: String,
    var amount: Long,
    val session: GdkSession
) {
    // Get asset from Session/AssetManager as it can be updated
    private val asset
        get() = session.getAsset(assetId)

    private val isPolicyAsset by lazy { assetId.isPolicyAsset(session) }

    suspend fun balance(
        denomination: Denomination? = null,
        withUnit: Boolean = false,
        withMinimumDigits: Boolean = true
    ): String = amount.toAmountLookOrNa(
        session = session,
        assetId = assetId,
        denomination = denomination,
        withUnit = withUnit,
        withGrouping = true,
        withMinimumDigits = withMinimumDigits
    )

    val fiatValue: Balance?
        get() = if (isPolicyAsset) {
            runBlocking { session.convertAmount(assetId, Convert(satoshi = amount)) }
        } else {
            null
        }

    val name: String
        get() = assetId.getAssetName(session)
}

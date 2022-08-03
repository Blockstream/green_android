package com.blockstream.green.looks

import com.blockstream.gdk.data.Balance
import com.blockstream.gdk.params.Convert
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetName
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.utils.toAmountLookOrNa


class AssetLook constructor(
    val assetId: String,
    var amount: Long,
    val session: GdkSession
) {
    // Get asset from Session/AssetManager as it can be updated
    private val asset
        get() = session.getAsset(assetId)

    private val isPolicyAsset by lazy { assetId.isPolicyAsset(session) }

    fun balance(
        isFiat: Boolean? = null,
        withUnit: Boolean = false,
        withMinimumDigits: Boolean = true
    ): String = amount.toAmountLookOrNa(
        session,
        assetId = assetId,
        isFiat = isFiat,
        withUnit = withUnit,
        withGrouping = true,
        withMinimumDigits = withMinimumDigits
    )

    val fiatValue: Balance?
        get() = if (isPolicyAsset) {
            session.convertAmount(assetId, Convert(satoshi = amount))
        } else {
            null
        }

    val name: String
        get() = assetId.getAssetName(session)
}

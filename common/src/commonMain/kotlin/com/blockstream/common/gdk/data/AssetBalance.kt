package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.utils.toAmountLook
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AssetBalance constructor(
    val asset: EnrichedAsset,
    val balance: String? = null,
    val balanceExchange: String? = null
) : GreenJson<AssetBalance>(), Parcelable {
    override fun kSerializer() = serializer()

    val assetId
        get() = asset.assetId

    companion object {
        suspend fun create(
            assetId: String,
            balance: Long,
            session: GdkSession,
            denomination: Denomination? = null
        ): AssetBalance {

            return AssetBalance(
                asset = EnrichedAsset.create(session, assetId),
                balance = balance.toAmountLook(
                    session = session,
                    assetId = assetId,
                    withUnit = true,
                    denomination = denomination
                ),
                balanceExchange = balance.toAmountLook(
                    session = session,
                    assetId = assetId,
                    withUnit = true,
                    denomination = Denomination.exchange(session, denomination)
                )
            )
        }
    }
}
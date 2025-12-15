package com.blockstream.data.gdk.data

import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.utils.toAmountLook
import kotlinx.serialization.Serializable

@Serializable
data class AssetBalanceList(val list: List<AssetBalance>)

@Serializable
data class AssetBalance(
    val asset: EnrichedAsset,
    val balance: String? = null,
    val balanceExchange: String? = null
) : GreenJson<AssetBalance>() {
    override fun kSerializer() = serializer()

    val assetId
        get() = asset.assetId

    companion object {

        fun create(
            asset: EnrichedAsset
        ): AssetBalance = AssetBalance(asset = asset)

        suspend fun create(
            assetId: String,
            balance: Long? = null,
            session: GdkSession,
            denomination: Denomination? = null
        ): AssetBalance {
            return AssetBalance(
                asset = EnrichedAsset.create(session, assetId),
                balance = session.starsOrNull ?: balance?.toAmountLook(
                    session = session,
                    assetId = assetId,
                    withUnit = true,
                    denomination = denomination
                ),
                balanceExchange = balance?.toAmountLook(
                    session = session,
                    assetId = assetId,
                    withUnit = true,
                    denomination = Denomination.exchange(session, denomination)
                )?.let { session.starsOrNull ?: it }
            )
        }
    }
}
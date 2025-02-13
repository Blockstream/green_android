package com.blockstream.common.gdk.data

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class AccountAsset constructor(
    val account: Account,
    val asset: EnrichedAsset
) : GreenJson<AccountAsset>() {
    override fun kSerializer() = serializer()

    val assetId
        get() = asset.assetId

    val accountAssetBalance
        get() = AccountAssetBalance.create(this)

    fun balance(session: GdkSession) = session.accountAssets(account).value.balance(assetId)

    companion object {
        fun fromAccountAsset(account: Account, assetId: String, session: GdkSession): AccountAsset {
            return AccountAsset(account, EnrichedAsset.create(session, assetId))
        }
    }
}
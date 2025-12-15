package com.blockstream.data.gdk.data

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class AccountAssetList constructor(val list: List<AccountAsset>)

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

    val assetBalance
        get() = AssetBalance.create(this.asset)

    fun balance(session: GdkSession) = session.accountAssets(account).value.balance(assetId)

    companion object {
        fun fromAccountAsset(account: Account, assetId: String, session: GdkSession): AccountAsset {
            return AccountAsset(account, EnrichedAsset.create(session, assetId))
        }
    }
}

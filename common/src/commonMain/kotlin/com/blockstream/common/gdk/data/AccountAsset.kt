package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.GdkSession
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AccountAsset constructor(
    val account: Account,
    val asset: EnrichedAsset
) : Parcelable {

    val assetId
        get() = asset.assetId

    fun balance(session: GdkSession) =
        session.accountAssets(account).value.assets.firstNotNullOfOrNull { accountAssets ->
            accountAssets.value.takeIf { accountAssets.key == asset.assetId }
        } ?: 0L

    companion object {
        fun fromAccountAsset(account: Account, assetId: String, session: GdkSession): AccountAsset {
            return AccountAsset(account, EnrichedAsset.create(session, assetId))
        }
    }
}
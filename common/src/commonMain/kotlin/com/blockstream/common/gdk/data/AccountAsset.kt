package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.extensions.getAssetName
import com.blockstream.common.extensions.getAssetTicker
import com.blockstream.common.gdk.GdkSession
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AccountAsset constructor(
    val account: Account,
    val assetId: String
) : Parcelable {

    fun balance(session: GdkSession) = session.accountAssets(account).value.assets.firstNotNullOfOrNull { asset ->
        asset.value.takeIf { asset.key == assetId }
    } ?: 0L

    fun asset(session: GdkSession) = session.getAsset(assetId)
    fun assetName(session: GdkSession) = assetId.getAssetName(session)
    fun assetTicker(session: GdkSession) = assetId.getAssetTicker(session)

    companion object{
        fun fromAccount(account: Account): AccountAsset {
            return AccountAsset(account, account.network.policyAsset)
        }
    }
}
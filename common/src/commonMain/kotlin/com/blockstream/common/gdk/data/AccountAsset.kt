package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AccountAsset constructor(
    val account: Account,
    val assetId: String
) : Parcelable {
    companion object{
        fun fromAccount(account: Account): AccountAsset {
            return AccountAsset(account, account.network.policyAsset)
        }
    }
}
package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    // Decided to switch xpub_hash_id / wallet_hash_id as xpub hash makes more sense to our use case
    @SerialName("xpub_hash_id") val walletHashId: String, // Use xpub id as the wallet hash id
    @SerialName("wallet_hash_id") val networkHashId: String
): GdkJson<LoginData>() {

    override fun kSerializer() = serializer()
}
package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    // Decided to switch xpub_hash_id / wallet_hash_id as xpub hash makes more sense to our use case
    @SerialName("xpub_hash_id") val walletHashId: String, // Use xpub id as the wallet hash id
    @SerialName("wallet_hash_id") val networkHashId: String
): GAJson<LoginData>() {

    override fun kSerializer(): KSerializer<LoginData> {
        return serializer()
    }
}
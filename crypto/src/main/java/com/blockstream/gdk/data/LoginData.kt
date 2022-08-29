package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    @SerialName("wallet_hash_id") val walletHashId: String,
    @SerialName("xpub_hash_id") val xPubHashId: String
): GAJson<LoginData>() {

    override fun kSerializer(): KSerializer<LoginData> {
        return serializer()
    }
}
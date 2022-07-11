package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Credentials constructor(
    @SerialName("mnemonic") val mnemonic: String,
    @SerialName("bip39_passphrase") val bip39Passphrase: String? = null,
    @SerialName("seed") val seed: String? = null,
) : GAJson<Credentials>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}
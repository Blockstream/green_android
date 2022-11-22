package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.GAJson
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Credentials constructor(
    @SerialName("mnemonic") val mnemonic: String,
    @SerialName("bip39_passphrase") val bip39Passphrase: String? = null,
    @SerialName("seed") val seed: String? = null,
) : GAJson<Credentials>(), Parcelable {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}
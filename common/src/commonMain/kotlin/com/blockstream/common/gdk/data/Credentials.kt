package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.params.LoginCredentialsParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Credentials constructor(
    @SerialName("mnemonic") val mnemonic: String,
    @SerialName("bip39_passphrase") val bip39Passphrase: String? = null,
    @SerialName("seed") val seed: String? = null,
) : GreenJson<Credentials>(), Parcelable {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    companion object{

        fun empty() = Credentials(mnemonic = "")

        fun fromLoginCredentialsParam(loginCredentialsParams: LoginCredentialsParams) : Credentials? {
            return loginCredentialsParams.mnemonic?.let {
                Credentials(
                    mnemonic = it,
                    bip39Passphrase = loginCredentialsParams.bip39Passphrase,
                )
            }
        }
    }
}
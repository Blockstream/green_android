package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.params.LoginCredentialsParams
import kotlinx.parcelize.IgnoredOnParcel
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

    @IgnoredOnParcel
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()

    companion object{

        fun fromLoginCredentialsParam(loginCredentialsParams: LoginCredentialsParams) : Credentials {
            return Credentials(
                mnemonic = loginCredentialsParams.mnemonic ?: "",
                bip39Passphrase = loginCredentialsParams.bip39Passphrase,
            )
        }
    }
}
package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GdkJson
import com.blockstream.common.gdk.params.LoginCredentialsParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Credentials constructor(
    @SerialName("mnemonic") val mnemonic: String,
    @SerialName("bip39_passphrase") val bip39Passphrase: String? = null,
    @SerialName("seed") val seed: String? = null,
) : GdkJson<Credentials>(), Parcelable {

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
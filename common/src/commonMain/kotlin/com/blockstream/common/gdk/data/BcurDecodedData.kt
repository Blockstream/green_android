package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.jade.JadeResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class BcurDecodedData(
    @SerialName("ur_type") val urType: String,
    @SerialName("data") val data: String? = null,
    @SerialName("psbt") val psbt: String? = null,
    @SerialName("descriptor") val descriptor: String? = null,
    @SerialName("descriptors") val descriptors: List<String>? = null,
    @SerialName("master_fingerprint") val masterFingerprint: String? = null,
    @SerialName("encrypted") val encrypted: String? = null,
    @SerialName("public_key") val publicΚey: String? = null,
    @SerialName("result") val result: JadeResponse? = null,
): Parcelable {

    val simplePayload : String
        get() = descriptors?.joinToString(",") ?: descriptor ?: psbt ?: data ?: ""
}


package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.jade.JadeResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BcurDecodedData(
    @SerialName("ur_type")
    val urType: String,
    @SerialName("data")
    val data: String? = null,
    @SerialName("psbt")
    val psbt: String? = null,
    @SerialName("descriptor")
    val descriptor: String? = null,
    @SerialName("descriptors")
    val descriptors: List<String>? = null,
    @SerialName("master_fingerprint")
    val masterFingerprint: String? = null,
    @SerialName("encrypted")
    val encrypted: String? = null,
    @SerialName("public_key")
    val publicΚey: String? = null,
    @SerialName("result")
    val result: JadeResponse? = null,
) : GreenJson<BcurDecodedData>() {
    override fun kSerializer() = serializer()

    val simplePayload: String
        get() = descriptors?.joinToString(",") ?: descriptor ?: psbt ?: data ?: ""
}


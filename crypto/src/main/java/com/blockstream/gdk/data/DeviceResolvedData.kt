package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceResolvedData constructor(
    @SerialName("xpubs") val xpubs: List<String?>? = null,

    @SerialName("signer_commitment") val signerCommitment: String? = null,
    @SerialName("signature") val signature: String? = null,

    @SerialName("signer_commitments") val signerCommitments: List<String?>? = null,
    @SerialName("signatures") val signatures: List<String?>? = null,

    @SerialName("asset_commitments") val assetCommitments: List<String?>? = null,
    @SerialName("value_commitments") val valueCommitments: List<String?>? = null,
    @SerialName("assetblinders") val assetBlinders: List<String?>? = null, // no underscore in the json field
    @SerialName("amountblinders") val amountBlinders: List<String?>? = null, // no underscore in the json field

    @SerialName("master_blinding_key") val masterBlindingKey: String? = null,

    @SerialName("nonces") val nonces: List<String?>? = null,
    @SerialName("public_keys") val publicKeys: List<String?>? = null,

): GAJson<DeviceResolvedData>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}
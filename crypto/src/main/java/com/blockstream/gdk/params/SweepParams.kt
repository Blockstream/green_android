package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.Addressee
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class SweepParams constructor(
    @SerialName("fee_rate") val feeRate: Long,
    @SerialName("private_key") val privateKey: String,
    @SerialName("passphrase") val passphrase: String,
    @SerialName("addressees") val addressees: List<AddressParams>,
    @SerialName("subaccount") val subAccount: Long
): GAJson<SweepParams>() {
    override fun kSerializer(): KSerializer<SweepParams> = serializer()
}
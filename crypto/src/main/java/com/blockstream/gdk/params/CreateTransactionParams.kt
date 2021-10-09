package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateTransactionParams(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("addressees") val addressees: List<AddressParams>, // This can also be a BIP21 URI
    @SerialName("utxos") val utxos: JsonElement? = null,
) : GAJson<CreateTransactionParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<CreateTransactionParams> {
        return serializer()
    }
}

@Serializable
data class AddressParams(
    @SerialName("address") val address: String,
    @SerialName("asset_id") val assetId: String? = null,
) : GAJson<AddressParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer() = serializer()
}
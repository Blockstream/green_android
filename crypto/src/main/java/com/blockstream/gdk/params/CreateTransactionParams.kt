package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// TODO untested with v4

@Serializable
data class CreateTransactionParams(
    @SerialName("subaccount") val subaccount: Int,
    @SerialName("addressees") val addressees: List<String>,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("utxos") val utxos: JsonElement? = null,
    @SerialName("fee_rate") val feeRate: Long? = null,
) : GAJson<CreateTransactionParams>() {
    override val encodeDefaultsValues = false

    override fun kSerializer(): KSerializer<CreateTransactionParams> {
        return serializer()
    }
}
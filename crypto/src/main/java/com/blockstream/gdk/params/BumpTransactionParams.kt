package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class BumpTransactionParams(
    @SerialName("subaccount") val subAccount: Long,
    @SerialName("utxos") val utxos: JsonElement,
    @SerialName("previous_transaction") val previousTransaction: JsonElement
): GAJson<BumpTransactionParams>() {

    override fun kSerializer(): KSerializer<BumpTransactionParams> = serializer()
}

package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class BumpTransactionParams(
    @SerialName("previous_transaction") val previousTransaction: JsonElement,
    @SerialName("fee_rate") val feeRate: Long,
    @SerialName("subaccount") val subAccount: Long
): GAJson<BumpTransactionParams>() {

    override fun kSerializer(): KSerializer<BumpTransactionParams> = serializer()
}

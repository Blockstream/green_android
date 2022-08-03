package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.LiquiDexV0List
import com.blockstream.gdk.data.Utxo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CompleteSwapParams(
    @SerialName("swap_type") val swapType: String = "liquidex",
    @SerialName("input_type") val inputType: String = "liquidex_v0",
    @SerialName("output_type") val output_type: String = "transaction",

    @SerialName("liquidex_v0") val liquidexV0: LiquiDexV0List,
    @SerialName("utxos") val utxos: JsonElement,
) : GAJson<CompleteSwapParams>() {
    override val encodeDefaultsValues = true

    override fun kSerializer(): KSerializer<CompleteSwapParams> {
        return serializer()
    }
}
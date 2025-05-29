package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.LiquiDexV0List
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CompleteSwapParams(
    @SerialName("swap_type")
    val swapType: String = "liquidex",
    @SerialName("input_type")
    val inputType: String = "liquidex_v0",
    @SerialName("output_type")
    val output_type: String = "transaction",

    @SerialName("liquidex_v0")
    val liquidexV0: LiquiDexV0List,
    @SerialName("utxos")
    val utxos: JsonElement,
) : GreenJson<CompleteSwapParams>() {

    override fun kSerializer() = serializer()
}
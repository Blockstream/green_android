package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSwapParams constructor(
    @SerialName("swap_type") val swapType: String = "liquidex",
    @SerialName("input_type") val inputType: String = "liquidex_v0",
    @SerialName("output_type") val output_type: String = "liquidex_v0",

    @SerialName("liquidex_v0") val liquidexV0: LiquidDexV0Params,
) : GAJson<CreateSwapParams>() {
    override val encodeDefaultsValues = true

    override fun kSerializer(): KSerializer<CreateSwapParams> {
        return serializer()
    }
}
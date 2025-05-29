package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSwapParams constructor(
    @SerialName("swap_type")
    val swapType: String = "liquidex",
    @SerialName("input_type")
    val inputType: String = "liquidex_v0",
    @SerialName("output_type")
    val output_type: String = "liquidex_v0",

    @SerialName("liquidex_v0")
    val liquidexV0: LiquidDexV0Params,
) : GreenJson<CreateSwapParams>() {

    override fun kSerializer() = serializer()
}
package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LiquidDexV0Params constructor(
    @SerialName("receive")
    val receive: List<LiquidDexV0AssetParams>,
    @SerialName("send")
    val send: List<JsonElement>,
) : GreenJson<LiquidDexV0Params>() {

    override fun kSerializer() = serializer()
}

@Serializable
data class LiquidDexV0AssetParams constructor(
    @SerialName("asset_id")
    val assetId: String,
    @SerialName("satoshi")
    val satoshi: Long,
) : GreenJson<LiquidDexV0AssetParams>() {

    override fun kSerializer() = serializer()
}

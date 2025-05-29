package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSwapTransaction constructor(
    @SerialName("liquidex_v0")
    val liquiDexV0: LiquiDexV0,
) : GreenJson<CreateSwapTransaction>() {
    override fun kSerializer() = serializer()
}
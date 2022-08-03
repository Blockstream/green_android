package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSwapTransaction constructor(
    @SerialName("liquidex_v0") val liquiDexV0: LiquiDexV0,
) : GAJson<CreateSwapTransaction>() {
    override fun kSerializer(): KSerializer<CreateSwapTransaction> = serializer()
}
package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UnspentOutputs(
    @SerialName("unspent_outputs") val unspentOutputs: JsonElement
)
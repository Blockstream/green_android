package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TwoFactorStatus(
    @SerialName("action") val action: String,
    @SerialName("methods") val methods: List<String> = listOf(),
    @SerialName("method") val method: String = "",
    @SerialName("status") val status: String,
    @SerialName("result") val result: JsonElement? = null,
    @SerialName("error") val error: String? = null,

    @SerialName("device") val device: Device? = null,
    @SerialName("required_data") val requiredData: String? = null,
)

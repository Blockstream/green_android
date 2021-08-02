package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.fasterxml.jackson.databind.ObjectMapper
import com.greenaddress.greenapi.data.TwoFactorStatusData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class TwoFactorStatus(
    @SerialName("action") val action: String,
    @SerialName("methods") val methods: List<String> = listOf(),
    @SerialName("method") val method: String = "",
    @SerialName("status") val status: String,
    @SerialName("result") val result: JsonElement? = null,
    @SerialName("error") val error: String? = null,

    @SerialName("attempts_remaining") val attemptsRemaining: Int? = null,
    @SerialName("required_data") val requiredData: DeviceRequiredData? = null,

    // Wait for a fix #535
    @SerialName("auth_data") val authData: JsonElement? = null,
) : GAJson<TwoFactorStatus>() {

    override fun kSerializer(): KSerializer<TwoFactorStatus> {
        return serializer()
    }

    // Used to convert object to Jackson based classes
    var rawJsonElement: JsonElement? = null

    private val objectMapper by lazy { ObjectMapper() }

    fun getTwoFactorStatusDataV3(): TwoFactorStatusData {
        return objectMapper.treeToValue(
            objectMapper.readTree(Json.encodeToString(rawJsonElement)),
            TwoFactorStatusData::class.java
        )
    }
}

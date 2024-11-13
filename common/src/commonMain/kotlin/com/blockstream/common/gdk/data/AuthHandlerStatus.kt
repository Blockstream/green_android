package com.blockstream.common.gdk.data

import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AuthHandlerStatus constructor(
    @SerialName("action") val action: String,
    @SerialName("methods") val methods: List<String> = listOf(),
    @SerialName("method") val method: String? = null,
    @SerialName("status") val status: String,
    @SerialName("result") val result: JsonElement? = null,
    @SerialName("error") val error: String? = null,

    @SerialName("attempts_remaining") val attemptsRemaining: Int? = null,
    @SerialName("required_data") val requiredData: DeviceRequiredData? = null,

    // Wait for a fix #535
    @SerialName("auth_data") val authData: JsonElement? = null,
) : GreenJson<AuthHandlerStatus>() {
    override fun keepJsonElement() = true

    override fun kSerializer() = serializer()

    fun isSms() = method == "sms"

    val progress: Int?
        get() = tryCatchNull { authData?.jsonObject?.get("estimated_progress")?.jsonPrimitive?.intOrNull }

    companion object {
        fun from(jsonString: String): AuthHandlerStatus = Json.decodeFromString(jsonString)
    }
}

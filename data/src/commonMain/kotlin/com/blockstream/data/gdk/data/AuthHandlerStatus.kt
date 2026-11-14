package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.JsonConverter.Companion.JsonDeserializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthHandlerStatus constructor(
    @SerialName("action")
    val action: String,
    @SerialName("methods")
    val methods: List<String> = listOf(),
    @SerialName("method")
    val method: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("result")
    val result: JsonElement? = null,
    @SerialName("error")
    val error: String? = null,

    @SerialName("attempts_remaining")
    val attemptsRemaining: Int? = null,
    @SerialName("required_data")
    val requiredData: DeviceRequiredData? = null,

    @SerialName("auth_data")
    val authData: AuthData? = null,
) : GreenJson<AuthHandlerStatus>() {
    override fun keepJsonElement() = true

    override fun kSerializer() = serializer()

    fun isSms() = method == "sms"

    companion object {
        fun from(jsonString: String): AuthHandlerStatus = JsonDeserializer.decodeFromString(jsonString)
    }
}

// GDK emits auth_data either as an object or as a bare boolean
@Serializable(with = AuthDataSerializer::class)
sealed class AuthData {
    @Serializable
    data class Data(
        @SerialName("telegram_url")
        val telegramUrl: String? = null,
        @SerialName("estimated_progress")
        val estimatedProgress: Int? = null,
    ) : AuthData()

    @Serializable(with = EnabledSerializer::class)
    data class Enabled(val value: Boolean) : AuthData()
}

object AuthDataSerializer : JsonContentPolymorphicSerializer<AuthData>(AuthData::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AuthData> =
        if (element is JsonObject) AuthData.Data.serializer() else EnabledSerializer
}

object EnabledSerializer : KSerializer<AuthData.Enabled> {
    override val descriptor = PrimitiveSerialDescriptor("AuthDataEnabled", PrimitiveKind.BOOLEAN)
    override fun deserialize(decoder: Decoder) = AuthData.Enabled(decoder.decodeBoolean())
    override fun serialize(encoder: Encoder, value: AuthData.Enabled) = encoder.encodeBoolean(value.value)
}

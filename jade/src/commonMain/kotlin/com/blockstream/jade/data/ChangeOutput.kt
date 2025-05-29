package com.blockstream.jade.data

import com.blockstream.jade.api.JadeSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ChangeOutputSerializer::class)
@KeepGeneratedSerializer
data class ChangeOutput public constructor(
    val path: List<Long>? = null,
    @SerialName("recovery_xpub")
    val recoveryXpub: String? = null,
    @SerialName("csv_blocks")
    val csvBlocks: Int? = null,
    val variant: String? = null,
) : JadeSerializer<ChangeOutput>() {
    override fun kSerializer() = serializer()
}

// Use this custom serializer so that we get real null values instead of empty map values eg. [{}]
object ChangeOutputSerializer : KSerializer<ChangeOutput> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ChangeOutput", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): ChangeOutput {
        return decoder.decodeSerializableValue(ChangeOutput.generatedSerializer())
    }

    override fun serialize(encoder: Encoder, value: ChangeOutput) {
        encoder.encodeSerializableValue(ChangeOutput.generatedSerializer(), value)
    }
}
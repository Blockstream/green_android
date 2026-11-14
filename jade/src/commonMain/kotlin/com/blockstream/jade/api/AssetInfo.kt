// Liquid asset registry metadata passed to Jade in sign_liquid_tx so it can display
// the ticker, issuer domain and precision-formatted amounts of non-policy assets.
// Jade recomputes the asset id from the contract and issuance prevout, so the contract
// must be sent exactly as published by the registry, keys in their original order.
package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

@Serializable
data class AssetInfo(
    @SerialName("asset_id")
    val assetId: String,
    @Serializable(with = ContractSerializer::class)
    val contract: JsonObject,
    @SerialName("issuance_prevout")
    val issuancePrevout: IssuancePrevout,
) : JadeSerializer<AssetInfo>() {
    override fun kSerializer() = serializer()
    override fun encodeDefaultsValues() = false
}

@Serializable
data class IssuancePrevout(
    val txid: String,
    val vout: Int,
)

// Encodes a JsonElement tree through any format encoder (kotlinx's own JsonElement
// serializers only work with the Json format). Map entries are written in iteration
// order, which for a parsed JsonObject is the order of the source document.
// The descriptor is deliberately not of class kind: the CBOR encoder writes a null
// class-kind element as an empty map instead of a CBOR null.
object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JsonElement", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonElement) {
        when (value) {
            is JsonObject -> encoder.encodeSerializableValue(MapSerializer(String.serializer(), this), value)
            is JsonArray -> encoder.encodeSerializableValue(ListSerializer(this), value)
            is JsonNull -> encoder.encodeNull()
            is JsonPrimitive -> when {
                value.isString -> encoder.encodeString(value.content)
                value.booleanOrNull != null -> encoder.encodeBoolean(value.booleanOrNull!!)
                value.longOrNull != null -> encoder.encodeLong(value.longOrNull!!)
                else -> encoder.encodeDouble(value.double)
            }
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement = (decoder as JsonDecoder).decodeJsonElement()
}

object ContractSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor = JsonElementSerializer.descriptor

    override fun serialize(encoder: Encoder, value: JsonObject) = JsonElementSerializer.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): JsonObject = JsonElementSerializer.deserialize(decoder).jsonObject
}

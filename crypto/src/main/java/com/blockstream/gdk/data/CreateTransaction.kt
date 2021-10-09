package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CreateTransaction(
    @SerialName("addressees") val addressees: List<Addressee>,
    @SerialName("error") val error: String?
) : GAJson<CreateTransaction>() {
    override val keepJsonElement = true

    override fun kSerializer(): KSerializer<CreateTransaction> = serializer()

    private val objectMapper by lazy { ObjectMapper() }

    fun toObjectNode(): ObjectNode {
        return objectMapper.readTree(Json.encodeToString(jsonElement)) as ObjectNode
    }
}

@Serializable
data class Addressee(
    @SerialName("address") val address: String,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("bip21-params") val bip21Params: Bip21Params? = null,
) : GAJson<Addressee>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class Bip21Params(
    @SerialName("amount") val amount: String? = null,
    @SerialName("assetid") val assetId: String? = null,
    @SerialName("bip21-params") val bip21Params: String? = null,
) : GAJson<Bip21Params>() {
    override fun kSerializer() = serializer()
}
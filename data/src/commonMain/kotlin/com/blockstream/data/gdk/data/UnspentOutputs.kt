package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class UnspentOutputs(
    @SerialName("unspent_outputs")
    val unspentOutputs: Map<String, List<JsonElement>>
) : GreenJson<UnspentOutputs>() {
    override fun keepJsonElement() = true

    val unspentOutputsAsUtxo: Map<String, List<Utxo>> by lazy {
        unspentOutputs.mapValues {
            it.value.map { utxo ->
                json.decodeFromJsonElement(utxo)
            }
        }
    }

    override fun kSerializer(): KSerializer<UnspentOutputs> = serializer()
}

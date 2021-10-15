package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class UnspentOutputs(
    @SerialName("unspent_outputs") val unspentOutputs: Map<String, List<Utxo>>
) : GAJson<UnspentOutputs>() {
    override val keepJsonElement = true

    val unspentOutputsAsJsonElement: JsonElement
        get() = jsonElement!!.jsonObject["unspent_outputs"]!!

    override fun kSerializer(): KSerializer<UnspentOutputs> = serializer()
}

package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class UnspentOutputs(
    @SerialName("unspent_outputs") val unspentOutputs: Map<String, List<Utxo>>
) : GAJson<UnspentOutputs>() {
    override val keepJsonElement = true

    val unspentOutputsAsJsonElement: JsonElement
        get() = jsonElement!!.jsonObject["unspent_outputs"]!!

    fun fillUtxosJsonElement(){
        unspentOutputs.forEach { (k, utxos) ->
            val jsonArray = unspentOutputsAsJsonElement.jsonObject[k]!!.jsonArray
            utxos.forEachIndexed { index, utxo ->
                utxo.jsonElement = jsonArray[index].jsonObject
            }
        }
    }

    override fun kSerializer(): KSerializer<UnspentOutputs> = serializer()

    operator fun plus(other: UnspentOutputs): UnspentOutputs {

        val newKeys = this.unspentOutputs.keys + other.unspentOutputs.keys

        val newUnspentOutputs = newKeys.associateWith { key ->
            // combine utxos
            (this.unspentOutputs[key] ?: listOf()) + (other.unspentOutputs[key] ?: listOf())
        }

        // rebuild json
        val newJsonElement = buildJsonObject {
            putJsonObject("unspent_outputs") {
                newKeys.forEach { key ->
                    val newUnspentOutputsAsJsonElement = (unspentOutputsAsJsonElement.jsonObject[key]?.jsonArray ?: buildJsonArray {  }) + (other.unspentOutputsAsJsonElement.jsonObject[key]?.jsonArray ?: buildJsonArray {  })
                    putJsonArray(key){
                        newUnspentOutputsAsJsonElement.forEach { add(it) }
                    }
                }
            }
        }

        return UnspentOutputs(unspentOutputs = newUnspentOutputs).also {
            it.jsonElement = newJsonElement
        }
    }
}

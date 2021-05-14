package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


@Serializable
data class Networks(
    @SerialName("networks") val networks: Map<String, Network>,
) {

    val bitcoinGreen by lazy { getNetworkById("mainnet") }
    val liquidGreen by lazy { getNetworkById("liquid") }
    val testnetGreen by lazy { getNetworkById("testnet") }

    val bitcoinElectrum by lazy { getNetworkById("electrum-mainnet") }
    val liquidElectrum by lazy { getNetworkById("electrum-liquid") }
    val testnetElectrum by lazy { getNetworkById("electrum-testnet") }


    fun getNetworkById(id: String): Network {
        return networks[id] ?: bitcoinGreen
    }

    companion object {
        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(json: Json, element: JsonElement): Networks {

            val networks: MutableMap<String, JsonObject> = mutableMapOf<String, JsonObject>()

            element.jsonObject["all_networks"]?.jsonArray?.let{
                for (key in it){
                    @Suppress("NAME_SHADOWING")
                    val key = key.jsonPrimitive.content
                    element.jsonObject[key]?.jsonObject?.let{ obj ->
                        networks[key] = buildJsonObject {
                            put("id", key)
                            for(k in obj){
                                put(k.key, k.value)
                            }
                        }
                    }
                }
            }

            return json.decodeFromJsonElement(buildJsonObject {
                putJsonObject("networks") {
                    for(k in networks){
                        put(k.key, k.value)
                    }
                }
            })
        }

    }
}
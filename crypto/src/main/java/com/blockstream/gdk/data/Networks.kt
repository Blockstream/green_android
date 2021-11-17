package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.GreenWallet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


@Serializable
data class Networks(
    @SerialName("networks") val networks: MutableMap<String, Network>
) : GAJson<Networks>() {

    override val keepJsonElement = true

    val bitcoinGreen by lazy { getNetworkById(Network.GreenMainnet) }
    val liquidGreen by lazy { getNetworkById(Network.GreenLiquid) }
    val testnetGreen by lazy { getNetworkById(Network.GreenTestnet) }
    val testnetLiquidGreen by lazy { getNetworkById(Network.GreenTestnetLiquid) }

    val bitcoinElectrum by lazy { getNetworkById(Network.ElectrumMainnet) }
    val liquidElectrum by lazy { getNetworkById(Network.ElectrumLiquid) }
    val testnetElectrum by lazy { getNetworkById(Network.ElectrumTestnet) }
    val testnetLiquidElectrum by lazy { getNetworkById(Network.ElectrumTestnetLiquid) }

    val hardwareSupportedNetworks by lazy { listOf(bitcoinGreen, liquidGreen, testnetGreen) }

    val customNetwork: Network?
        get() = getNetworkByIdOrNull(CustomNetworkId)

    fun getNetworkById(id: String): Network {
        return networks[id] ?: throw Exception("Network '$id' is not available in the current build of GDK")
    }

    private fun getNetworkByIdOrNull(id: String): Network? {
        return networks[id]
    }

    fun getNetworkAsJsonElement(id: String): JsonElement? = jsonElement?.jsonObject?.get("networks")?.jsonObject?.get(id)

    fun setCustomNetwork(network: Network) {
        networks[network.id] = network
    }

    fun getNetworkByType(networkTypeOrId: String, isElectrum: Boolean): Network {
        return when (networkTypeOrId) {
            Network.GreenMainnet, Network.ElectrumMainnet -> {
                if (isElectrum) bitcoinElectrum else bitcoinGreen
            }
            Network.GreenLiquid, Network.ElectrumLiquid  -> {
                if (isElectrum) liquidElectrum else liquidGreen
            }
            Network.GreenTestnetLiquid, Network.ElectrumTestnetLiquid -> {
                if (isElectrum) testnetLiquidElectrum else testnetLiquidGreen
            }
            else -> { // Network.GreenTestnet, Network.ElectrumTestnet
                if (isElectrum) testnetElectrum else testnetGreen
            }
        }
    }

    companion object {
        const val CustomNetworkId = "custom-network"

        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(json: Json, element: JsonElement): Networks {

            val networks: MutableMap<String, JsonObject> = mutableMapOf()

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

            return buildJsonObject {
                putJsonObject("networks") {
                    for(k in networks){
                        put(k.key, k.value)
                    }
                }
            }.let { jsonObject ->
                json.decodeFromJsonElement<Networks>(jsonObject).apply {
                    if(keepJsonElement) {
                        jsonElement = jsonObject
                    }
                }
            }
        }

    }

    override fun kSerializer(): KSerializer<Networks> = serializer()
}
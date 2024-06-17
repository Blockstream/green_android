package com.blockstream.common.gdk.data

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


@Serializable
data class Networks(
    @SerialName("networks") val networks: Map<String, Network>
) : GreenJson<Networks>() {
    override fun kSerializer(): KSerializer<Networks> = serializer()

    override fun keepJsonElement() = true

    val lightning by lazy {
        Network(
            id = Network.LightningMainnet,
            network = Network.LightningMainnet,
            name = "Lightning",
            isMainnet = true,
            isLiquid = false,
            isDevelopment = false,
            explorerUrl = bitcoinElectrum.explorerUrl,
            bip21Prefix = "lightning",
            policyAsset = BTC_POLICY_ASSET,
            isLightning = true
        )
    }

    val bitcoinGreen by lazy { getNetworkById(Network.GreenMainnet) }
    val liquidGreen by lazy { getNetworkById(Network.GreenLiquid) }
    val testnetBitcoinGreen by lazy { getNetworkById(Network.GreenTestnet) }
    val testnetLiquidGreen by lazy { getNetworkById(Network.GreenTestnetLiquid) }

    val bitcoinElectrum by lazy { getNetworkById(Network.ElectrumMainnet) }
    val liquidElectrum by lazy { getNetworkById(Network.ElectrumLiquid) }
    val testnetBitcoinElectrum by lazy { getNetworkById(Network.ElectrumTestnet) }
    val testnetLiquidElectrum by lazy { getNetworkById(Network.ElectrumTestnetLiquid) }

    val customNetwork: Network?
        get() = getNetworkByIdOrNull(CustomNetworkId)

    fun bitcoinElectrum(isTestnet: Boolean) = if(isTestnet) testnetBitcoinElectrum else bitcoinElectrum
    fun liquidElectrum(isTestnet: Boolean) = if(isTestnet) testnetLiquidElectrum else liquidElectrum

    fun getNetworkById(id: String): Network {
        return getNetworkByIdOrNull(id) ?: throw Exception("Network '$id' is not available in the current build of GDK")
    }

    private fun getNetworkByIdOrNull(id: String): Network? {
        return networks[id]
    }

    fun getNetworkAsJsonElement(id: String): JsonElement? = jsonElement?.jsonObject?.get("networks")?.jsonObject?.get(id)

    fun getNetworkByType(networkTypeOrId: String, isElectrum: Boolean): Network {
        if(networkTypeOrId == CustomNetworkId){
            return customNetwork!!
        }

        return when (networkTypeOrId) {
            Network.GreenMainnet, Network.ElectrumMainnet -> {
                if (isElectrum) bitcoinElectrum else bitcoinGreen
            }
            Network.GreenLiquid, Network.ElectrumLiquid -> {
                if (isElectrum) liquidElectrum else liquidGreen
            }
            Network.GreenTestnetLiquid, Network.ElectrumTestnetLiquid -> {
                if (isElectrum) testnetLiquidElectrum else testnetLiquidGreen
            }
            else -> { // Network.GreenTestnet, Network.ElectrumTestnet
                if (isElectrum) testnetBitcoinElectrum else testnetBitcoinGreen
            }
        }
    }

    fun getNetworkByAccountType(networkTypeOrId: String, accountType: AccountType): Network {
        if(networkTypeOrId == CustomNetworkId){
            return customNetwork!!
        }

        return when(accountType){
            // Multisig
            AccountType.STANDARD, AccountType.AMP_ACCOUNT, AccountType.TWO_OF_THREE -> {
                if(Network.isBitcoin(networkTypeOrId)){
                    if(Network.isBitcoinMainnet(networkTypeOrId)) bitcoinGreen else testnetBitcoinGreen
                }else{
                    if(Network.isLiquidMainnet(networkTypeOrId)) liquidGreen else testnetLiquidGreen
                }
            }else -> {
                // Singlesig
                if(Network.isBitcoin(networkTypeOrId)){
                    if(Network.isBitcoinMainnet(networkTypeOrId)) bitcoinElectrum else testnetBitcoinElectrum
                }else{
                    if(Network.isLiquidMainnet(networkTypeOrId)) liquidElectrum else testnetLiquidElectrum
                }
            }
        }
    }

    companion object {
        const val CustomNetworkId = "custom-network"

        fun fromJsonString(jsonString: String): Networks {
            return fromJsonElement(json.parseToJsonElement(jsonString))
        }
        /**
        Transform the gdk json to a more appropriate format
         */
        fun fromJsonElement(element: JsonElement): Networks {

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
                    if(keepJsonElement()) {
                        jsonElement = jsonObject
                    }
                }
            }
        }
    }
}
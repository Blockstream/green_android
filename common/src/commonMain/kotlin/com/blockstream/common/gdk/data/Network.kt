package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Network(
    @SerialName("id") val id: String, // this is a synthetic property app side
    @SerialName("name") val name: String,
    @SerialName("network") val network: String,
    @SerialName("mainnet") val isMainnet: Boolean,
    @SerialName("liquid") val isLiquid: Boolean,
    @SerialName("development") val isDevelopment: Boolean,
    @SerialName("default_peers") val defaultPeers: List<String> = listOf(),
    @SerialName("bip21_prefix") val bip21Prefix: String = "network",
    @SerialName("tx_explorer_url") val explorerUrl: String? = null,
    @SerialName("policy_asset") val policyAsset: String = BTC_POLICY_ASSET,
    @SerialName("server_type") val serverType: String? = null,
    @SerialName("csv_buckets") val csvBuckets: List<Int> = listOf(),
    @SerialName("lightning") val isLightning: Boolean = false, // synthetic
) : GreenJson<Network>(), Parcelable {

    val isElectrum
        get() = "electrum" == serverType

    val isSinglesig
        get() = isElectrum

    val isMultisig
        get() = !isElectrum && !isLightning

    val isTestnet
        get() = !isMainnet

    val isBitcoin
        get() = !isLiquid && !isLightning

    val isBitcoinOrLightning
        get() = !isLiquid

    val isBitcoinMainnet
        get() = isBitcoinMainnet(id)

    val isLiquidMainnet
        get() = isLiquidMainnet(id)

    val isBitcoinTestnet
        get() = isBitcoinTestnet(id)

    val isLiquidTestnet
        get() = isLiquidTestnet(id)

    val canonicalName: String
        get() = when(network){
                GreenMainnet, ElectrumMainnet -> "Bitcoin"
                GreenTestnet, ElectrumTestnet -> "Testnet"
                GreenLiquid, ElectrumLiquid -> "Liquid"
                GreenTestnetLiquid, ElectrumTestnetLiquid -> "Testnet Liquid"
                else -> name
            }

    val canonicalNetworkId: String
        get() = canonicalNetworkId(network)

    val productName: String
        get() = if (isElectrum) {
            "Singlesig $canonicalName"
        } else {
            "Multisig $canonicalName"
        }

    val policyAssetOrNull
        get() = if(isLiquid) policyAsset else null

    val countlyId: String
        get() = if (isMultisig) {
            "legacy-$id"
        } else {
            id
        }

    val canSignMessage
        get() = isSinglesig && !isLiquid

    @IgnoredOnParcel
    val defaultFee by lazy {
        if (isLiquid) 100L else 1000L
    }

    @IgnoredOnParcel
    val blocksPerHour
        get() = if (isLiquid) 60 else 6

    @IgnoredOnParcel
    val confirmationsRequired: Long
        get() = if(isLiquid) 2L else 6L

    fun getVerPublic(): Int {
        return if (isMainnet) BIP32_VER_MAIN_PUBLIC else BIP32_VER_TEST_PUBLIC
    }

    fun isSameNetwork(other: Network): Boolean =
        this.isBitcoin && other.isBitcoin || this.isLightning && other.isLightning || this.isLiquid && other.isLiquid

    override fun kSerializer() = serializer()

    companion object{
        const val GreenMainnet = "mainnet"
        const val GreenLiquid = "liquid"
        const val GreenTestnet = "testnet"
        const val GreenTestnetLiquid = "testnet-liquid"

        const val ElectrumMainnet = "electrum-mainnet"
        const val ElectrumLiquid = "electrum-liquid"
        const val ElectrumTestnet = "electrum-testnet"
        const val ElectrumTestnetLiquid = "electrum-testnet-liquid"

        const val LightningMainnet = "greenlight-mainnet"

        const val BIP32_VER_MAIN_PUBLIC = 0x0488B21E
        const val BIP32_VER_TEST_PUBLIC = 0x043587CF

        fun canonicalNetworkId(id: String) = id.removePrefix("electrum-")

        fun isSinglesig(id: String) = id.contains("electrum")
        fun isMultisig(id: String) = !isSinglesig(id) && !isLightning(id)

        fun isBitcoinMainnet(id: String) = (id == GreenMainnet || id == ElectrumMainnet)
        fun isLiquidMainnet(id: String) = (id == GreenLiquid || id == ElectrumLiquid)

        fun isBitcoinTestnet(id: String) = (id == GreenTestnet || id == ElectrumTestnet)
        fun isLiquidTestnet(id: String) = (id == GreenTestnetLiquid || id == ElectrumTestnetLiquid)

        fun isLightning(id: String) = isLightningMainnet(id)
        fun isLightningMainnet(id: String) = id == LightningMainnet

        fun isLiquid(id: String) = isLiquidMainnet(id) || isLiquidTestnet(id)
        fun isBitcoin(id: String) = isBitcoinMainnet(id) || isBitcoinTestnet(id)
        fun isTestnet(id: String) = isBitcoinTestnet(id) || isLiquidTestnet(id)
    }
}
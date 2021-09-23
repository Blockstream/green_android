package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.crypto.R
import com.blockstream.gdk.GAJson
import com.blockstream.libwally.Wally
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KLogging

@Serializable
@Parcelize
data class Network(
    @SerialName("id") val id: String, // this is a synthetic property by us
    @SerialName("name") val name: String,
    @SerialName("network") val network: String,
    @SerialName("mainnet") val isMainnet: Boolean,
    @SerialName("liquid") val isLiquid: Boolean,
    @SerialName("development") val isDevelopment: Boolean,
    @SerialName("default_peers") val defaultPeers: List<String> = listOf(),
    @SerialName("tx_explorer_url") val explorerUrl: String? = null,
    @SerialName("policy_asset") val policyAsset: String = "btc",
    @SerialName("server_type") val serverType: String? = null,
    @SerialName("csv_buckets") val csvBuckets: List<Int> = listOf(),
) : GAJson<Network>(), Parcelable {

    val isElectrum
        get() = "electrum" == serverType

    val isSinglesig
        get() = isElectrum

    val isMultisig
        get() = !isElectrum

    val supportTorConnection
        get () = !isElectrum

    val isTestnet
        get() = !isMainnet

    val canonicalName: String
        get() = when(network){
                GreenMainnet, ElectrumMainnet -> "Bitcoin"
                GreenTestnet, ElectrumTestnet -> "Testnet"
                GreenLiquid, ElectrumLiquid -> "Liquid"
                GreenTestnetLiquid, ElectrumTestnetLiquid -> "Testnet Liquid"
                else -> name
            }

    val productName: String
        get() = if (isElectrum) {
            "Singlesig $canonicalName"
        } else {
            "Multisig $canonicalName"
        }

    val defaultFee = 1000L

    fun getVerPublic(): Int {
        return if (isMainnet) Wally.BIP32_VER_MAIN_PUBLIC else Wally.BIP32_VER_TEST_PUBLIC
    }

    fun getNetworkIcon(): Int{
        if (isMainnet(id)) return R.drawable.ic_bitcoin_network_60
        if (isLiquid(id)) return R.drawable.ic_liquid_network_60
        if (isTestnet(id)) return R.drawable.ic_bitcoin_testnet_network_60
        if (isTestnetLiquid(id)) return R.drawable.ic_liquid_testnet_network_60
        return R.drawable.ic_unknown_network_60
    }

    override fun kSerializer(): KSerializer<Network> = serializer()

    companion object: KLogging(){
        const val GreenMainnet = "mainnet"
        const val GreenLiquid = "liquid"
        const val GreenTestnet = "testnet"
        const val GreenTestnetLiquid = "testnet-liquid"

        const val ElectrumMainnet = "electrum-mainnet"
        const val ElectrumLiquid = "electrum-liquid"
        const val ElectrumTestnet = "electrum-testnet"
        const val ElectrumTestnetLiquid = "electrum-testnet-liquid"

        fun isMainnet(id: String) = (id == GreenMainnet || id == ElectrumMainnet)
        fun isLiquid(id: String) = (id == GreenLiquid || id == ElectrumLiquid)
        fun isTestnet(id: String) = (id == GreenTestnet || id == ElectrumTestnet)
        fun isTestnetLiquid(id: String) = (id == GreenTestnetLiquid || id == ElectrumTestnetLiquid)
    }
}
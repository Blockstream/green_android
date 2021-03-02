package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("wamp_onion_url") val wampOnioUrl: String? = null,

    @SerialName("policy_asset") val policyAsset: String? = null,
    @SerialName("server_type") val serverType: String? = null,
) : Parcelable {

    val isElectrum
        get() = "electrum" == serverType
}
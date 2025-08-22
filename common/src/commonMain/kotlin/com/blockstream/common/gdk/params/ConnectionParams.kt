package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionParams constructor(
    @SerialName("name")
    val networkName: String,
    @SerialName("use_tor")
    val useTor: Boolean,
    @SerialName("user_agent")
    val userAgent: String,
    @SerialName("proxy")
    val proxy: String,

    @SerialName("electrum_tls")
    val electrumTls: Boolean = true,
    @SerialName("electrum_url")
    val electrumUrl: String? = null,
    @SerialName("electrum_onion_url")
    val electrumOnionUrl: String? = null,
    @SerialName("blob_server_url")
    val blobServerUrl: String? = null,
    @SerialName("blob_server_onion_url")
    val blobServerOnionUrl: String? = null,
    @SerialName("gap_limit")
    val gapLimit: Int? = null
) : GreenJson<ConnectionParams>() {

    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()
}
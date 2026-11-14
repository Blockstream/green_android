package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
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

    companion object {
        /**
         * Routes a personal Electrum server between electrum_url and electrum_onion_url.
         *
         * GDK prefers electrum_onion_url when use_tor is set and always dials it without
         * TLS, which is only correct for actual onion services. A clearnet server stays in
         * electrum_url (dialed through the Tor proxy with electrum_tls honored); returning
         * an empty string overrides the network's default onion server so the personal
         * server is still the one used.
         */
        fun electrumOnionUrl(electrumUrl: String?, useTor: Boolean): String? = when {
            electrumUrl == null || !useTor -> null
            electrumUrl.substringBefore(":").endsWith(".onion", ignoreCase = true) -> electrumUrl
            else -> ""
        }
    }
}
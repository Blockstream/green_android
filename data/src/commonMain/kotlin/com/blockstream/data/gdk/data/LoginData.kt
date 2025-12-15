package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    // Decided to switch xpub_hash_id / wallet_hash_id as xpub hash makes more sense to our use case
    @SerialName("xpub_hash_id")
    val xpubHashId: String, // Use xpub id as the identifier id
    @SerialName("wallet_hash_id")
    val networkHashId: String,
    @SerialName("raw_watch_only_data")
    val rawWatchOnlyData: String? = null,
    @SerialName("watch_only_data")
    val watchOnlyData: String? = null,
    @SerialName("warnings")
    val warnings: List<String> = listOf()
) : GreenJson<LoginData>() {

    override fun kSerializer() = serializer()
}
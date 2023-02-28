package com.blockstream.green.database

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class WatchOnlyCredentials constructor(
    @SerialName("password") val password: String? = null,
    @SerialName("core_descriptors") val coreDescriptors: List<String>? = null,
    @SerialName("slip132_extended_pubkeys") val slip132ExtendedPubkeys: List<String>? = null,
): GAJson<WatchOnlyCredentials>() {
    override val encodeDefaultsValues: Boolean = false

    override fun kSerializer(): KSerializer<WatchOnlyCredentials> {
        return serializer()
    }

    companion object{
        fun fromByteArray(byteArray: ByteArray): WatchOnlyCredentials{
            return Json.decodeFromString(String(byteArray))
        }
    }
}
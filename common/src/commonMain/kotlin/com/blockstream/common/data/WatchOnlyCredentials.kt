package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WatchOnlyCredentials constructor(
    @SerialName("password") val password: String? = null,
    @SerialName("core_descriptors") val coreDescriptors: List<String>? = null,
    @SerialName("slip132_extended_pubkeys") val slip132ExtendedPubkeys: List<String>? = null,
): GreenJson<WatchOnlyCredentials>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    companion object{
        fun fromByteArray(byteArray: ByteArray): WatchOnlyCredentials {
            return Json.decodeFromString(byteArray.decodeToString())
        }
    }
}
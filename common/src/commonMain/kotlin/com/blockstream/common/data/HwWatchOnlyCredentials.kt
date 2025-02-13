package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.params.LoginCredentialsParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HwWatchOnlyCredentials constructor(
    @SerialName("credentials") val credentials: Map<String, WatchOnlyCredentials> = emptyMap(),
): GreenJson<HwWatchOnlyCredentials>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    fun toLoginCredentials() = LoginCredentialsParams(hwWatchOnlyCredentials = this)

    companion object{
        fun fromByteArray(byteArray: ByteArray): HwWatchOnlyCredentials {
            return Json.decodeFromString(byteArray.decodeToString())
        }

        fun fromWatchOnlyCredentials(network: String, watchOnlyCredentials : WatchOnlyCredentials): HwWatchOnlyCredentials {
            return HwWatchOnlyCredentials(
                mapOf(network to watchOnlyCredentials)
            )
        }
    }
}
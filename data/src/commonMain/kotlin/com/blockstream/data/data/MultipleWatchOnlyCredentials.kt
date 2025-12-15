package com.blockstream.data.data

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.params.LoginCredentialsParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MultipleWatchOnlyCredentials constructor(
    @SerialName("credentials")
    val credentials: Map<String, WatchOnlyCredentials> = emptyMap(),
) : GreenJson<MultipleWatchOnlyCredentials>() {
    override fun encodeDefaultsValues() = false

    override fun kSerializer() = serializer()

    fun toLoginCredentials() = LoginCredentialsParams(multipleWatchOnlyCredentials = this)

    fun isHwWatchOnly() = credentials.values.all { it.username.isNullOrBlank() && it.password.isNullOrBlank() }

    fun isCoreDescriptors() = credentials.values.all { it.coreDescriptors != null }

    companion object Companion {
        fun fromByteArray(byteArray: ByteArray): MultipleWatchOnlyCredentials {
            return Json.decodeFromString(byteArray.decodeToString())
        }

        fun fromWatchOnlyCredentials(network: String, watchOnlyCredentials: WatchOnlyCredentials): MultipleWatchOnlyCredentials {
            return MultipleWatchOnlyCredentials(
                mapOf(network to watchOnlyCredentials)
            )
        }
    }
}
package com.blockstream.data.data

import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

@Serializable
data class AppKeys(
    @SerialName("breez_api_key")
    val breezApiKey: String? = null,
    @SerialName("greenlight_key")
    val greenlightKey: String? = null,
    @SerialName("greenlight_cert")
    val greenlightCert: String? = null,
    @SerialName("zendesk_client_id")
    val zendeskClientId: String? = null,
    @SerialName("reown_project_id")
    val reownProjectId: String? = null,
) : GreenJson<AppKeys>() {
    override fun kSerializer() = serializer()

    companion object {
        fun fromText(text: String): AppKeys? = text.takeIf { it.isNotBlank() }?.let {
            try {
                val trimmed = it.trim()
                runCatching {
                    json.decodeFromString<AppKeys>(trimmed)
                }.getOrElse {
                    val normalized = trimmed.filterNot { ch -> ch.isWhitespace() }
                    json.decodeFromString(Base64.decode(normalized).decodeToString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

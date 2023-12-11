package com.blockstream.common.data

import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.internal.commonToUtf8String
import kotlin.io.encoding.Base64

@Serializable
data class AppSecrets(
    @SerialName("breez_api_key") val breezApiKey: String?,
    @SerialName("greenlight_key") val greenlightKey: String?,
    @SerialName("greenlight_cert") val greenlightCert: String?,
    @SerialName("zendesk_client_id") val zendeskClientId: String?,
) : GreenJson<AppSecrets>() {
    override fun kSerializer() = serializer()

    companion object {
        fun fromText(text: String): AppSecrets? = text.takeIf { it.isNotBlank() }?.let {
            try {
                json.decodeFromString(Base64.decode(it).commonToUtf8String())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

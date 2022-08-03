package com.blockstream.green.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Banner(
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("title") val title: String? = null,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("message") val message: String? = null,
    @SerialName("dismissable") val dismissable: Boolean? = null,
    @SerialName("is_warning") val isWarning: Boolean = false,
    @SerialName("link") val link: String? = null,
    @SerialName("screens") val screens: List<String>? = null,
    @SerialName("networks") val networks: List<String>? = null,
): GAJson<Banner>(){
    override fun kSerializer() = serializer()
}
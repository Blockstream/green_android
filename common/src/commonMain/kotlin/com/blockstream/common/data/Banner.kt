package com.blockstream.common.data

import com.blockstream.common.gdk.GdkJson
import com.blockstream.common.serializers.HtmlEntitiesSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Banner(
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("title") val title: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("message") val message: String? = null,
    @SerialName("dismissable") val dismissable: Boolean? = null,
    @SerialName("is_warning") val isWarning: Boolean = false,
    @SerialName("link") val link: String? = null,
    @SerialName("screens") val screens: List<String>? = null,
    @SerialName("networks") val networks: List<String>? = null,
): GdkJson<Banner>(){
    override fun kSerializer() = serializer()

    val hasNetworks: Boolean
        get() = networks != null
}
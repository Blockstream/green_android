package com.blockstream.data.gdk.data

import com.blockstream.data.serializers.Base64Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiquidAssets(
    @SerialName("assets")
    val assets: Map<String, Asset>? = null,
    @SerialName("icons")
    val icons: Map<String, @Serializable(with = Base64Serializer::class) ByteArray?>? = null,
)
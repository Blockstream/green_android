package com.blockstream.gdk.data

import android.graphics.Bitmap
import com.blockstream.gdk.serializers.BitmapSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Assets(
    @SerialName("assets") val assets: Map<String, Asset>? = null,
    @SerialName("icons") val icons: Map<String, @Serializable(with = BitmapSerializer::class) Bitmap?>? = null,
)
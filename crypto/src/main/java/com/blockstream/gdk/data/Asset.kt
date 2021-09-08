package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Asset constructor(
    @SerialName("name") val name: String,
    @SerialName("asset_id") val assetId: String,
    @SerialName("precision") val precision: Int = 0,
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("entity") val entity: Entity = Entity(""),
) : Parcelable

@Serializable
@Parcelize
data class Entity(@SerialName("domain") val domain: String) : Parcelable


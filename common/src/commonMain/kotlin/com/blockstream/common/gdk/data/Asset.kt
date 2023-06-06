package com.blockstream.common.gdk.data


import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class Asset constructor(
    @SerialName("name") val name: String,
    @SerialName("asset_id") val assetId: String,
    @SerialName("precision") val precision: Int = 0,
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("entity") val entity: Entity? = null,
) : GdkJson<Asset>(), Parcelable {

    override fun kSerializer() = serializer()

    companion object {
        fun createEmpty(assetId: String) = Asset(name = assetId, assetId = assetId, precision = 0)
    }
}

@Serializable
@Parcelize
data class Entity(@SerialName("domain") val domain: String) : Parcelable


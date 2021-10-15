package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.params.Convert
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KLogging

@Serializable
@Parcelize
data class Asset constructor(
    @SerialName("name") val name: String,
    @SerialName("asset_id") val assetId: String,
    @SerialName("precision") val precision: Int = 0,
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("entity") val entity: Entity = Entity(""),
) : GAJson<Asset>(), Parcelable{

    override fun kSerializer() = serializer()

    companion object:KLogging(){
        fun createEmpty(assetId: String) = Asset(name = assetId, assetId = assetId, precision = 0)
    }
}

@Serializable
@Parcelize
data class Entity(@SerialName("domain") val domain: String) : Parcelable


package com.blockstream.data.gdk.data

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.jade.api.IssuancePrevout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Asset constructor(
    @SerialName("name")
    val name: String,
    @SerialName("asset_id")
    val assetId: String,
    @SerialName("precision")
    val precision: Int = 0,
    @SerialName("ticker")
    val ticker: String? = null,
    @SerialName("entity")
    val entity: Entity? = null,
    // Registry contract as published, needed by Jade to authenticate the asset metadata
    @SerialName("contract")
    val contract: JsonObject? = null,
    @SerialName("issuance_prevout")
    val issuancePrevout: IssuancePrevout? = null,
) : GreenJson<Asset>() {

    val isBitcoin
        get() = assetId == BTC_POLICY_ASSET

    override fun kSerializer() = serializer()

    companion object {
        val BTC by lazy { createEmpty(BTC_POLICY_ASSET) }

        fun createEmpty(assetId: String) = Asset(name = assetId, assetId = assetId, precision = 0)

        suspend fun create(assetId: String, session: GdkSession) = session.getAsset(assetId) ?: createEmpty(assetId)
    }
}

@Serializable
data class Entity(
    @SerialName("domain")
    val domain: String
)


package com.blockstream.common.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Entity
import com.blockstream.common.gdk.data.Network
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class EnrichedAsset constructor(
    @SerialName("asset_id") val assetId: String,
    @SerialName("name") val name: String? = null,
    @SerialName("precision") val precision: Int = 0,
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("entity") val entity: Entity? = null,
    @SerialName("amp") val isAmp: Boolean = false,
    @SerialName("weight") val weight: Int = 0,
    @SerialName("isSendable") val isSendable: Boolean = false, // Display "Any Liquid Asset" UI element
    @SerialName("isAnyAsset") val isAnyAsset: Boolean = false, // Display "Any Liquid/Amp Asset" UI element
) : GreenJson<EnrichedAsset>(), Parcelable {


    fun nameOrNull(session: GdkSession?): String? {
        return if (isAnyAsset) {
            if (isAmp) "id_receive_any_amp_asset" else "id_receive_any_liquid_asset"
        } else if (session != null && assetId.isPolicyAsset(session)) {
            if (assetId == BTC_POLICY_ASSET) {
                "Bitcoin"
            } else {
                "Liquid Bitcoin"
            }.let {
                if (session.isTestnet) "Testnet $it" else it
            }
        } else {
            name
        }
    }

    fun name(session: GdkSession?): String = nameOrNull(session) ?: assetId

    fun ticker(session: GdkSession): String? {
        return if (assetId.isPolicyAsset(session)) {
            if (assetId == BTC_POLICY_ASSET) {
                "BTC"
            } else {
                "L-BTC"
            }.let {
                if (session.isTestnet) "TEST-$it" else it
            }
        } else {
            ticker
        }
    }

    fun sortWeight(session: GdkSession) = when {
        assetId == BTC_POLICY_ASSET -> Int.MAX_VALUE
        isLiquid(session) -> Int.MAX_VALUE - 1
        isSendable -> Int.MIN_VALUE
        isAnyAsset -> Int.MIN_VALUE + 1
        else -> {
            val hasAssetIcon = session.hasAssetIcon(assetId)

            if (hasAssetIcon) {
                weight // from Countly
            } else if (name != null) {
                -10_000
            } else {
                -100_000
            }
        }
    }

    val isBitcoin
        get() = assetId == BTC_POLICY_ASSET

    fun isLiquid(session: GdkSession) = !isAnyAsset && assetId.isPolicyAsset(session)

    override fun kSerializer() = serializer()

    companion object {
        val Empty by lazy { EnrichedAsset(assetId = BTC_POLICY_ASSET) }

        // Untested, only used in Preview
        val PreviewBTC by lazy { EnrichedAsset(assetId = BTC_POLICY_ASSET, name = "Bitcoin", ticker = "BTC") }

        fun createOrNull(session: GdkSession, assetId: String?): EnrichedAsset? {
            return create(session, assetId ?: return null)
        }

        fun create(account: Account): EnrichedAsset = EnrichedAsset(assetId = account.network.policyAsset)

        fun create(session: GdkSession, network: Network): EnrichedAsset = create(session = session, assetId = network.policyAsset)

        fun create(session: GdkSession, assetId: String): EnrichedAsset {
            val asset = session.getAsset(assetId)
            val enrichedAsset = session.getEnrichedAssets(assetId)

            return EnrichedAsset(
                assetId = assetId,
                name = asset?.name,
                precision = asset?.precision ?: 0,
                ticker = asset?.ticker,
                entity = asset?.entity,

                isAmp = enrichedAsset?.isAmp ?: false,
                weight = enrichedAsset?.weight ?: 0,
            )
        }

        fun createAnyAsset(session: GdkSession, isAmp: Boolean): EnrichedAsset? {
            val assetId = session.liquid?.policyAsset ?: return null
            val asset = session.getAsset(assetId)

            return EnrichedAsset(
                assetId = assetId,
                name = asset?.name,
                precision = asset?.precision ?: 0,
                ticker = asset?.ticker,
                entity = asset?.entity,

                weight = if(isAmp) -20 else -10,
                isSendable = false,
                isAmp = isAmp,
                isAnyAsset = true
            )
        }
    }
}


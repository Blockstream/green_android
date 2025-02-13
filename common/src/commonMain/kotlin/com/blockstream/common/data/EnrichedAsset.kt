package com.blockstream.common.data

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_receive_any_amp_asset
import blockstream_green.common.generated.resources.id_receive_any_liquid_asset
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.LBTC_POLICY_ASSET
import com.blockstream.common.extensions.isBitcoinPolicyAsset
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Entity
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.utils.StringHolder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class EnrichedAsset constructor(
    @SerialName("asset_id") val assetId: String,
    @SerialName("name") val name: String? = null,
    @SerialName("precision") val precision: Int = 0,
    @SerialName("ticker") val ticker: String? = null,
    @SerialName("entity") val entity: Entity? = null,
    @SerialName("amp") val isAmp: Boolean = false,
    @SerialName("weight") val weight: Int = 0,
    // @SerialName("isSendable") val isSendable: Boolean = true, // Display "Any Liquid Asset" UI element
    @SerialName("isAnyAsset") val isAnyAsset: Boolean = false, // Display "Any Liquid/Amp Asset" UI element
) : GreenJson<EnrichedAsset>() {

    fun nameOrNull(session: GdkSession?): StringHolder? {
        return if (isAnyAsset) {
            StringHolder(stringResource = if (isAmp) Res.string.id_receive_any_amp_asset else Res.string.id_receive_any_liquid_asset)
        } else if (session != null && assetId.isPolicyAsset(session)) {
            when {
                assetId.isBitcoinPolicyAsset() -> "Bitcoin"
                assetId.isPolicyAsset(session.liquid) -> "Liquid Bitcoin"
                else -> throw Exception("No supported network")
            }.let {
                StringHolder(string = if (session.isTestnet) "Testnet $it" else it)
            }
        } else {
            name?.let { StringHolder.create(it) }
        }
    }

    fun name(session: GdkSession?): StringHolder = nameOrNull(session) ?: StringHolder(string = assetId)

    fun ticker(session: GdkSession): String? {
        return if (assetId.isPolicyAsset(session)) {
            when{
                assetId.isBitcoinPolicyAsset() -> "BTC"
                assetId.isPolicyAsset(session.liquid) -> "L-BTC"
                else -> throw Exception("No supported network")
            }.let {
                if (session.isTestnet) "TEST-$it" else it
            }
        } else {
            ticker
        }
    }

    fun sortWeight(session: GdkSession) = when {
        assetId == BTC_POLICY_ASSET -> Int.MAX_VALUE
        isLiquidPolicyAsset(session) -> Int.MAX_VALUE - 1
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

    fun isLiquidPolicyAsset(session: GdkSession) = !isAnyAsset && assetId.isPolicyAsset(session.liquid)

    fun isLiquidNetwork(session: GdkSession) = assetId.networkForAsset(session)?.isLiquid == true

    override fun kSerializer() = serializer()

    companion object {
        val Empty by lazy { EnrichedAsset(assetId = BTC_POLICY_ASSET) }

        // Untested, only used in Preview
        val PreviewBTC by lazy { EnrichedAsset(assetId = BTC_POLICY_ASSET, name = "Bitcoin", ticker = "BTC") }
        val PreviewLBTC by lazy { EnrichedAsset(assetId = LBTC_POLICY_ASSET, name = "Liquid Bitcoin", ticker = "L-BTC") }

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
                isAmp = isAmp,
                isAnyAsset = true
            )
        }
    }
}


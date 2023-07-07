package com.blockstream.common.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class EnrichedAsset constructor(
    @SerialName("id") val assetId: String,
    @SerialName("amp") val isAmp: Boolean = false,
    @SerialName("weight") val weight: Int = 0,
    @SerialName("isAnyLiquid") val isAnyLiquidAsset: Boolean = false
) : GdkJson<EnrichedAsset>() {
    override fun kSerializer() = serializer()

    companion object {
        fun create(assetId: String, isAmp: Boolean = false, weight: Int = 0) =
            EnrichedAsset(assetId = assetId, isAmp = isAmp, weight = weight)

        fun createOrNull(assetId: String?, isAmp: Boolean = false, weight: Int = 0) =
            if (assetId != null) EnrichedAsset(
                assetId = assetId,
                isAmp = isAmp,
                weight = weight
            ) else null
    }
}
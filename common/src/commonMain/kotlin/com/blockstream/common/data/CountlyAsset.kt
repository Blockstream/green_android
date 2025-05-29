package com.blockstream.common.data

import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountlyAsset constructor(
    @SerialName("id")
    val assetId: String,
    @SerialName("amp")
    val isAmp: Boolean = false,
    @SerialName("weight")
    val weight: Int = 0,
//    @SerialName("isSendable") val isSendable: Boolean = true, // Display "Any Liquid Asset" UI element/**/
    @SerialName("isAnyLiquid")
    val isAnyLiquidAsset: Boolean = false, // Display "Any Liquid Asset" UI element
    @SerialName("isAnyAmp")
    val isAnyAmpAsset: Boolean = false // Display "Any AMP Asset" UI element
) : GreenJson<CountlyAsset>() {
    override fun kSerializer() = serializer()

    val isAnyAsset
        get() = isAnyLiquidAsset || isAnyAmpAsset

    fun isLiquid(session: GdkSession) = !isAnyAsset && assetId.isPolicyAsset(session)

    companion object {
        fun anyLiquidAsset(session: GdkSession) = session.liquid?.let {
            CountlyAsset( // Any Liquid Asset
                assetId = it.policyAsset,
                weight = -10,
//                isSendable = false,
                isAnyLiquidAsset = true
            )
        }

        fun anyAmpAsset(session: GdkSession) = session.liquid?.let {
            CountlyAsset( // Any Liquid Asset
                assetId = it.policyAsset,
                weight = -20,
//                isSendable = false,
                isAmp = true,
                isAnyAmpAsset = true
            )
        }

        fun create(assetId: String, isAmp: Boolean = false, weight: Int = 0) =
            CountlyAsset(assetId = assetId, isAmp = isAmp, weight = weight)

        fun createOrNull(assetId: String?, isAmp: Boolean = false, weight: Int = 0) =
            if (assetId != null) CountlyAsset(
                assetId = assetId,
                isAmp = isAmp,
                weight = weight
            ) else null
    }
}
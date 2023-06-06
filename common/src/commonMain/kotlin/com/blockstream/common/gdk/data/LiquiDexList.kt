package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LiquiDexV0List(
    @SerialName("proposals") val proposals: List<SwapProposal>,
) : GdkJson<LiquiDexV0List>() {
    override fun kSerializer() = serializer()
}

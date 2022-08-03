package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LiquiDexV0List(
    @SerialName("proposals") val proposals: List<SwapProposal>,
) : GAJson<LiquiDexV0List>() {
    override fun kSerializer(): KSerializer<LiquiDexV0List> = serializer()
}

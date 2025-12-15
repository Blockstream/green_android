package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiquiDexV0List(
    @SerialName("proposals")
    val proposals: List<SwapProposal>,
) : GreenJson<LiquiDexV0List>() {
    override fun kSerializer() = serializer()
}

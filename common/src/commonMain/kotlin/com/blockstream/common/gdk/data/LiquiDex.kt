package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class LiquiDexV0 constructor(
    @SerialName("proposal")
    val proposalJsonElement: JsonElement,
) : GreenJson<LiquiDexV0>() {

    fun toSwapProposal(): SwapProposal {
        return json.decodeFromJsonElement<SwapProposal>(proposalJsonElement).also {
            it.proposal = proposalJsonElement.toString()
        }
    }

    override fun kSerializer() = serializer()
}

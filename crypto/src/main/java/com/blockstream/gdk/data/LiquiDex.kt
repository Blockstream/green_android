package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement


@Serializable
data class LiquiDexV0 constructor(
    @SerialName("proposal") val proposalJsonElement: JsonElement,
) : GAJson<LiquiDexV0>() {

    fun toSwapProposal(): SwapProposal {
        return json.decodeFromJsonElement<SwapProposal>(proposalJsonElement).also {
            it.proposal = proposalJsonElement.toString()
        }
    }

    override fun kSerializer(): KSerializer<LiquiDexV0> = serializer()
}

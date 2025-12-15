package com.blockstream.data.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HerokuResponse(
    @SerialName("proposal_id")
    val proposalId: String? = null
) : GreenJson<HerokuResponse>() {
    override fun kSerializer() = serializer()
}
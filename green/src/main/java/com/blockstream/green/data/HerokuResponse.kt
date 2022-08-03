package com.blockstream.green.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class HerokuResponse(
    @SerialName("proposal_id") val proposalId: String? = null
): GAJson<HerokuResponse>(){
    override fun kSerializer() = serializer()
}
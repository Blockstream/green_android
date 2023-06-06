package com.blockstream.common.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class HerokuResponse(
    @SerialName("proposal_id") val proposalId: String? = null
): GdkJson<HerokuResponse>(){
    override fun kSerializer() = serializer()
}
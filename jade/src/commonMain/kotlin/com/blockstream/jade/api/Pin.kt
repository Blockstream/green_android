package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PinRequestParams(
    @SerialName("data")
    val data: String
) : JadeSerializer<PinRequestParams>() {
    override fun kSerializer(): KSerializer<PinRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class PinRequest(
    override val id: String = jadeId(),
    override val method: String = "pin",
    override val params: PinRequestParams
) : Request<PinRequest, PinRequestParams>() {
    override fun kSerializer(): KSerializer<PinRequest> = kotlinx.serialization.serializer()
}

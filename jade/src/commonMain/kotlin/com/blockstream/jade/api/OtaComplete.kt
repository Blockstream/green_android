package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_AUTONOMOUS_LONG
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable


@Serializable
data class OtaCompleteRequest(
    override val id: String = jadeId(),
    override val method: String = "ota_complete",
    override val params: Unit = Unit
) : Request<OtaCompleteRequest, Unit>() {
    // ota_complete sometimes takes a bit more than 2 secs
    override fun timeout(): Int = TIMEOUT_AUTONOMOUS_LONG
    override fun kSerializer(): KSerializer<OtaCompleteRequest> = kotlinx.serialization.serializer()
}
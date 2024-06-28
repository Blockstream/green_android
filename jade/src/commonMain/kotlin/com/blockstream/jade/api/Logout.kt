package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable


@Serializable
data class LogoutRequest(
    override val id: String = jadeId(),
    override val method: String = "logout",
    override val params: Unit = Unit
) : Request<LogoutRequest, Unit>() {
    override fun kSerializer(): KSerializer<LogoutRequest> = kotlinx.serialization.serializer()
}

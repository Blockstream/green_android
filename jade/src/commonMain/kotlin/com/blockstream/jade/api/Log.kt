package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
class LogResponse(val log: String) : JadeSerializer<LogResponse>() {
    override fun kSerializer(): KSerializer<LogResponse> = kotlinx.serialization.serializer()
}
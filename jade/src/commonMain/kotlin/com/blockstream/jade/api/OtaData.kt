package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class OtaDataRequest(
    override val id: String = jadeId(),
    override val method: String = "ota_data",
    override val params: ByteArray
) : Request<OtaDataRequest, ByteArray>() {
    override fun timeout(): Int = TIMEOUT_USER_INTERACTION
    override fun kSerializer(): KSerializer<OtaDataRequest> = kotlinx.serialization.serializer()
}
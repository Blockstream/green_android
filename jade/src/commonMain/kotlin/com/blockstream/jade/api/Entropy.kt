package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
class EntropyRequestParams(
    val entropy: ByteArray
) : JadeSerializer<EntropyRequestParams>() {
    override fun kSerializer(): KSerializer<EntropyRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class EntropyRequest(
    override val id: String = jadeId(),
    override val method: String = "add_entropy",
    override val params: EntropyRequestParams
) : Request<EntropyRequest, EntropyRequestParams>() {
    override fun kSerializer(): KSerializer<EntropyRequest> = kotlinx.serialization.serializer()
}

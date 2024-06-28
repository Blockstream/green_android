package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignatureRequestParams(
    @SerialName("ae_host_entropy")
    val aeHostEntropy: ByteArray? = null,
) : JadeSerializer<SignatureRequestParams>() {
    override fun kSerializer(): KSerializer<SignatureRequestParams> = serializer()
}

@Serializable
data class SignatureRequest(
    override val id: String = jadeId(),
    override val method: String = "get_signature",
    override val params: SignatureRequestParams
) : Request<SignatureRequest, SignatureRequestParams>() {
    override fun kSerializer() = serializer()

    override val timeout: Int = TIMEOUT_USER_INTERACTION
}
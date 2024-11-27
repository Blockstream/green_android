package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignMessageRequestParams(
    val path: List<Long>,
    val message: String,
    @SerialName("ae_host_commitment")
    val aeHostCommitment: ByteArray? = null,
) : JadeSerializer<SignMessageRequestParams>() {
    override fun kSerializer(): KSerializer<SignMessageRequestParams> = serializer()
}

@Serializable
data class SignMessageRequest(
    override val id: String = jadeId(),
    override val method: String = "sign_message",
    override val params: SignMessageRequestParams
) : Request<SignMessageRequest, SignMessageRequestParams>() {

    override fun kSerializer(): KSerializer<SignMessageRequest> = kotlinx.serialization.serializer()

    override fun timeout(): Int = TIMEOUT_USER_INTERACTION
}

@OptIn(ExperimentalStdlibApi::class)
class SignedMessage(val signature: String, val signerCommitment: String? = null){
    constructor(signature: String, signerCommitment: ByteArray) : this(
        signature = signature,
        signerCommitment = signerCommitment.toHexString()
    )
}
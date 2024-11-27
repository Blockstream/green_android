package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignAttestation(
    val signature: ByteArray,
    @SerialName("pubkey_pem")
    val pubkeyPem: String,
    @SerialName("ext_signature")
    val extSignature: ByteArray,
) : JadeSerializer<SignAttestation>() {
    override fun kSerializer(): KSerializer<SignAttestation> = serializer()
}

@Serializable
data class SignAttestationRequestParams(
    val challenge: ByteArray,
) :
    JadeSerializer<SignAttestationRequestParams>() {
    override fun kSerializer(): KSerializer<SignAttestationRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class SignAttestationRequest(
    override val id: String = jadeId(),
    override val method: String = "sign_attestation",
    override val params: SignAttestationRequestParams
) : Request<SignAttestationRequest, SignAttestationRequestParams>() {
    override fun kSerializer(): KSerializer<SignAttestationRequest> = kotlinx.serialization.serializer()

    override fun timeout(): Int = TIMEOUT_USER_INTERACTION
}

@Serializable
data class SignAttestationResponse(
    override val id: String,
    override val result: SignAttestation? = null,
    override val error: Error? = null
) : Response<SignAttestationResponse, SignAttestation>() {
    override fun kSerializer(): KSerializer<SignAttestationResponse> =
        kotlinx.serialization.serializer()
}
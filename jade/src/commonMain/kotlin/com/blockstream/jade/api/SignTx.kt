package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import com.blockstream.jade.data.ChangeOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignTransactionRequestParams(
    val network: String,
    val txn: ByteArray,
    @SerialName("num_inputs")
    val numInput: Int,
    @SerialName("use_ae_signatures")
    val useAeSignatures: Boolean,
    @SerialName("trusted_commitments")
    val trustedCommitments: List<Commitment?>? = null,
    val change: List<ChangeOutput?>? = null,
) : JadeSerializer<SignTransactionRequestParams>() {
    override fun kSerializer(): KSerializer<SignTransactionRequestParams> = serializer()
    override fun encodeDefaultsValues(): Boolean = false
}

@Serializable
data class SignTransactionRequest(
    override val id: String,
    override val method: String,
    override val params: SignTransactionRequestParams
) : Request<SignTransactionRequest, SignTransactionRequestParams>() {
    override fun encodeDefaultsValues(): Boolean = false

    override val timeout: Int = TIMEOUT_USER_INTERACTION

    override fun kSerializer(): KSerializer<SignTransactionRequest> = kotlinx.serialization.serializer()
}

data class SignedTransaction(val signatures: List<String>, val signerCommitments: List<String>?)
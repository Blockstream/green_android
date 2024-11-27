package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MasterBlindingKeyRequestParams(
    @SerialName("only_if_silent")
    val onlyIfSilent: Boolean = false
) : JadeSerializer<MasterBlindingKeyRequestParams>() {
    override fun kSerializer(): KSerializer<MasterBlindingKeyRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class MasterBlindingKeyRequest(
    override val id: String = jadeId(),
    override val method: String = "get_master_blinding_key",
    override val params: MasterBlindingKeyRequestParams
) : Request<MasterBlindingKeyRequest, MasterBlindingKeyRequestParams>() {
    override fun kSerializer(): KSerializer<MasterBlindingKeyRequest> = kotlinx.serialization.serializer()

    override fun timeout(): Int = TIMEOUT_USER_INTERACTION
}
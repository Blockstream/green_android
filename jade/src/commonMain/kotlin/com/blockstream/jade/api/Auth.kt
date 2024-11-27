package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_NONE
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestParams(val network: String, val epoch: Long) :
    JadeSerializer<AuthRequestParams>() {
    override fun kSerializer(): KSerializer<AuthRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class AuthRequest(
    override val id: String = jadeId(),
    override val method: String = "auth_user",
    override val params: AuthRequestParams
) : Request<AuthRequest, AuthRequestParams>() {
    override fun kSerializer(): KSerializer<AuthRequest> = kotlinx.serialization.serializer()

    override fun timeout(): Int = TIMEOUT_NONE
}
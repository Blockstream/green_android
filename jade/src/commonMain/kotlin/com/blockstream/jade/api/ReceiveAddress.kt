package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiveAddressRequestParams(
    val network: String,
    val path: List<Long>? = null,
    val variant: String? = null,
    @SerialName("subaccount")
    val subAccount: Long? = null,
    val branch: Long? = null,
    val pointer: Long? = null,
    @SerialName("recovery_xpub")
    val recoveryXpub: String? = null,
    @SerialName("csv_blocks")
    val csvBlocks: Long? = null,
) : JadeSerializer<ReceiveAddressRequestParams>() {
    override fun kSerializer(): KSerializer<ReceiveAddressRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class ReceiveAddressRequest(
    override val id: String,
    override val method: String,
    override val params: ReceiveAddressRequestParams
) : Request<ReceiveAddressRequest, ReceiveAddressRequestParams>() {
    // Having encodeDefaultsValues = true needs id and method to be defined explicitly
    // else path in params will be null and this won't work with Jade
    override fun encodeDefaultsValues() = false

    override fun kSerializer(): KSerializer<ReceiveAddressRequest> = kotlinx.serialization.serializer()

    override fun timeout(): Int = TIMEOUT_USER_INTERACTION
}

package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class XpubRequestParams(val path: List<Long>, val network: String) : JadeSerializer<XpubRequestParams>() {
    override fun kSerializer(): KSerializer<XpubRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class XpubRequest(
    override val id: String = jadeId(),
    override val method: String = "get_xpub",
    override val params: XpubRequestParams
) : Request<XpubRequest, XpubRequestParams>() {
    override fun kSerializer(): KSerializer<XpubRequest> = kotlinx.serialization.serializer()
}
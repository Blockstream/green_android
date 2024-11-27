package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val code: Int,
    val message: String,
    val data: ByteArray? = null
) : JadeSerializer<Error>() {
    override fun kSerializer(): KSerializer<Error> = kotlinx.serialization.serializer()
}

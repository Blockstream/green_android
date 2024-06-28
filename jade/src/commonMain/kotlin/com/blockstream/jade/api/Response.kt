package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
sealed class Response<T, P> : JadeSerializer<T>() {
    abstract val id: String
    abstract val result: P?
    abstract val error: Error?
}

@Serializable
data class BooleanResponse(
    override val id: String,
    override val result: Boolean? = null,
    override val error: Error? = null
) : Response<BooleanResponse, Boolean>() {
    override fun kSerializer(): KSerializer<BooleanResponse> = kotlinx.serialization.serializer()
}

@Serializable
data class StringResponse(
    override val id: String,
    override val result: String? = null,
    override val error: Error? = null
) : Response<StringResponse, String>() {
    override fun kSerializer(): KSerializer<StringResponse> = kotlinx.serialization.serializer()
}

@Serializable
data class ByteArrayResponse(
    override val id: String,
    override val result: ByteArray? = null,
    override val error: Error? = null
) : Response<ByteArrayResponse, ByteArray>() {
    override fun kSerializer(): KSerializer<ByteArrayResponse> = kotlinx.serialization.serializer()
}
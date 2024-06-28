package com.blockstream.jade.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Jade returns "data" as a Map or as an empty string for handshake_init
// This breaks the CBOR modelling, so we have to use two models and normalize the return value

@Serializable
data class HttpRequestNoDataParams(
    @SerialName("urls")
    val urls: List<String>,
    @SerialName("method")
    val method: String,
    @SerialName("accept")
    val accept: String
) : JadeSerializer<HttpRequestNoDataParams>() {
    override fun kSerializer(): KSerializer<HttpRequestNoDataParams> = serializer()

    fun toHttpRequestParams(): HttpRequestParams = HttpRequestParams(urls = urls, method = method, accept = accept)
}

@Serializable
data class HttpRequestDataParams(
    @SerialName("urls")
    val urls: List<String>,
    @SerialName("method")
    val method: String,
    @SerialName("accept")
    val accept: String,
    @SerialName("data")
    val data: Map<String, String>? = null,
) : JadeSerializer<HttpRequestDataParams>() {
    override fun kSerializer(): KSerializer<HttpRequestDataParams> = serializer()

    fun toHttpRequestParams(): HttpRequestParams = HttpRequestParams(urls = urls, method = method, accept = accept, data = data)
}

@Serializable
data class HttpRequestNoData(
    @SerialName("params")
    val params: HttpRequestNoDataParams,
    @SerialName("on-reply")
    val onReply: String,
) : JadeSerializer<HttpRequestNoData>() {
    override fun kSerializer(): KSerializer<HttpRequestNoData> = serializer()

    fun toHttpRequest(): HttpRequest = HttpRequest(
        params = params.toHttpRequestParams(),
        onReply = onReply
    )
}

@Serializable
data class HttpRequestData(
    @SerialName("params")
    val params: HttpRequestDataParams,
    @SerialName("on-reply")
    val onReply: String,
) : JadeSerializer<HttpRequestData>() {
    override fun kSerializer(): KSerializer<HttpRequestData> = serializer()

    fun toHttpRequest(): HttpRequest = HttpRequest(
        params = params.toHttpRequestParams(),
        onReply = onReply
    )
}

@Serializable
data class HttpRequestNoDataResponseResult(
    @SerialName("http_request")
    val httpRequest: HttpRequestNoData
) : JadeSerializer<HttpRequestNoDataResponseResult>() {
    override fun kSerializer(): KSerializer<HttpRequestNoDataResponseResult> = serializer()

    fun toHttpRequestDataResponseResult(): HttpRequestResponseResult = HttpRequestResponseResult(
        httpRequest = httpRequest.toHttpRequest()
    )
}

@Serializable
data class HttpRequestDataResponseResult(
    @SerialName("http_request")
    val httpRequest: HttpRequestData
) : JadeSerializer<HttpRequestDataResponseResult>() {
    override fun kSerializer(): KSerializer<HttpRequestDataResponseResult> = serializer()

    fun toHttpRequestDataResponseResult(): HttpRequestResponseResult = HttpRequestResponseResult(
        httpRequest = httpRequest.toHttpRequest()
    )
}

@Serializable
data class HttpRequestNoDataResponse(
    override val id: String,
    override val result: HttpRequestNoDataResponseResult?,
    override val error: Error? = null
) : Response<HttpRequestNoDataResponse, HttpRequestNoDataResponseResult>() {
    override fun kSerializer(): KSerializer<HttpRequestNoDataResponse> = serializer()

    fun toHttpRequestDataResponse() = HttpRequestResponse(
        id = id,
        result = result?.toHttpRequestDataResponseResult(),
        error = error
    )
}

@Serializable
data class HttpRequestDataResponse(
    override val id: String,
    override val result: HttpRequestDataResponseResult?,
    override val error: Error? = null
) : Response<HttpRequestDataResponse, HttpRequestDataResponseResult>() {
    override fun kSerializer(): KSerializer<HttpRequestDataResponse> = serializer()

    fun toHttpRequestDataResponse() = HttpRequestResponse(
        id = id,
        result = result?.toHttpRequestDataResponseResult(),
        error = error
    )
}


// Normalized
@Serializable
data class HttpRequestParams constructor(
    @SerialName("urls")
    val urls: List<String>,
    @SerialName("method")
    val method: String,
    @SerialName("accept")
    val accept: String,
    @SerialName("data")
    val data: Map<String, String>? = null,
) : JadeSerializer<HttpRequestParams>() {
    override fun kSerializer(): KSerializer<HttpRequestParams> = serializer()
}

@Serializable
data class HttpRequest(
    @SerialName("params")
    val params: HttpRequestParams,
    @SerialName("on-reply")
    val onReply: String,
) : JadeSerializer<HttpRequest>() {
    override fun kSerializer(): KSerializer<HttpRequest> = serializer()
}

@Serializable
data class HttpRequestResponseResult(
    @SerialName("http_request")
    val httpRequest: HttpRequest
) : JadeSerializer<HttpRequestResponseResult>() {
    override fun kSerializer(): KSerializer<HttpRequestResponseResult> = serializer()
}

@Serializable
data class HttpRequestResponse(
    override val id: String,
    override val result: HttpRequestResponseResult?,
    override val error: Error? = null
) : Response<HttpRequestResponse, HttpRequestResponseResult>() {
    override fun kSerializer(): KSerializer<HttpRequestResponse> = serializer()
}
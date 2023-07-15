package com.blockstream.common.interfaces

import kotlinx.serialization.json.JsonElement

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
interface HttpRequestHandler {
    fun prepareHttpRequest()

    fun httpRequest(details: JsonElement): JsonElement

    fun httpRequest(
        method: String,
        urls: List<String>?,
        data: String?,
        accept: String?,
        certs: List<String>?
    ): JsonElement
}
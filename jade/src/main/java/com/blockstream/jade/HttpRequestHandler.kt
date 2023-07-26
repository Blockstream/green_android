package com.blockstream.jade

import kotlinx.serialization.json.JsonElement
import java.io.IOException
import java.net.URL

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
interface HttpRequestHandler {
    fun prepareHttpRequest()

    @Throws(IOException::class)
    fun httpRequest(details: JsonElement): JsonElement

    @Throws(IOException::class)
    fun httpRequest(
        method: String,
        urls: List<URL>?,
        data: String?,
        accept: String?,
        certs: List<String>?
    ): JsonElement
}
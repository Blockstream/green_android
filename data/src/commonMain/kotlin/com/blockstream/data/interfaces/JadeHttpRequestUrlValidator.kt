package com.blockstream.data.interfaces

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
interface JadeHttpRequestUrlValidator {
    suspend fun unsafeUrlWarning(urls: List<String>): Boolean
    suspend fun torWarning(): Boolean
}
package com.blockstream.common.interfaces

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
interface HttpRequestUrlValidator {
    fun unsafeUrlWarning(urls: List<String>): Boolean
}
package com.blockstream.common.interfaces

// Provides an HttpRequestHandler every time is required
// A provider pattern is usefull when support for multi-session are enabled.
interface HttpRequestProvider {
    val httpRequest: HttpRequestHandler
}
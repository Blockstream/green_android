package com.blockstream.green.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.darwin.Darwin

actual fun HttpClientFactory(block: HttpClientConfig<HttpClientEngineConfig>.() -> Unit): HttpClient {
    return HttpClient(Darwin) {
        block(this as HttpClientConfig<HttpClientEngineConfig>)
    }
}
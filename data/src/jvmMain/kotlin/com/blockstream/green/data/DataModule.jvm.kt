package com.blockstream.green.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

actual fun HttpClientFactory(block: HttpClientConfig<HttpClientEngineConfig>.() -> Unit): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation)
        block(this as HttpClientConfig<HttpClientEngineConfig>)
    }
}
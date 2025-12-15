package com.blockstream.data.meld

import com.blockstream.utils.Loggable
import com.blockstream.network.AppHttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MeldHttpClient(appInfo: com.blockstream.data.config.AppInfo) : AppHttpClient(appInfo.isDevelopmentOrDebug, {
    install(HttpTimeout) {
        this.requestTimeoutMillis = 60_000
        this.connectTimeoutMillis = 30_000
    }

    defaultRequest {
        url(MELD_PRODUCTION.takeIf { appInfo.isProduction }
            ?: MELD_SANDBOX)
        contentType(ContentType.Application.Json)
    }
}) {
    companion object : Loggable() {
        private const val MELD_PRODUCTION = "https://ramps.blockstream.com"
        private const val MELD_SANDBOX = "https://ramps.sandbox.blockstream.com"
    }
}
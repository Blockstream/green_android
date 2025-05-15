package com.blockstream.green.data.meld

import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.network.AppHttpClient
import com.blockstream.green.utils.Loggable
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MeldHttpClient(appInfo: AppInfo) : AppHttpClient(appInfo.isDevelopmentOrDebug, {
    install(HttpTimeout) {
        this.requestTimeoutMillis = 60_000
        this.connectTimeoutMillis = 30_000
    }

    defaultRequest {
        url(MELD_PRODUCTION.takeIf { appInfo.isProduction } ?: MELD_SANDBOX)
        contentType(ContentType.Application.Json)
    }
}) {
    companion object : Loggable() {
        private const val MELD_PRODUCTION = "https://ramps.blockstream.com"
        private const val MELD_SANDBOX = "https://ramps.sandbox.blockstream.com"
    }
}
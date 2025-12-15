package com.blockstream.data

import com.blockstream.data.config.AppInfo
import com.blockstream.network.AppHttpClient
import io.ktor.client.plugins.defaultRequest

class GreenWebhooksHttpClient(appInfo: AppInfo) : AppHttpClient(appInfo.isDevelopmentOrDebug, {
    defaultRequest {
        url(if (appInfo.isProduction) BASE_URL else DEV_BASE_URL)
    }
}) {
    companion object {
        const val BASE_URL = "https://green-webhooks.blockstream.com/"
        const val DEV_BASE_URL = "https://green-webhooks.dev.blockstream.com/"
    }
}
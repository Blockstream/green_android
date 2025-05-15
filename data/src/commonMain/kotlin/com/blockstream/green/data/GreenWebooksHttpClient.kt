package com.blockstream.green.data

import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.network.AppHttpClient
import io.ktor.client.plugins.defaultRequest

class GreenWebooksHttpClient(appInfo: AppInfo) : AppHttpClient(appInfo.isDevelopmentOrDebug, {
    defaultRequest {
        url(if (appInfo.isProduction) BASE_URL else DEV_BASE_URL)
    }
}) {
    companion object {
        const val BASE_URL = "https://green-webhooks.dev.blockstream.com/"
        const val DEV_BASE_URL = "https://green-webhooks.dev.blockstream.com/"
    }
}
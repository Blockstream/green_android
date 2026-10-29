// HTTP client for the blockstream.zendesk.com API. Uploads and requests are
// submitted anonymously, so the app holds no Zendesk credentials.

package com.blockstream.data.zendesk

import com.blockstream.data.config.AppInfo
import com.blockstream.network.AppHttpClient
import com.blockstream.network.NetworkResponse
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ZendeskHttpClient(appInfo: AppInfo, engine: HttpClientEngine? = null) : AppHttpClient(
    // Request bodies carry the user's email and log files, so only development and debug builds log them
    enableLogging = appInfo.isDevelopmentOrDebug,
    configBlock = {
        defaultRequest {
            url(BASE_URL)
        }
    },
    engine = engine
) {
    internal suspend fun uploadLog(filename: String, content: ByteArray): NetworkResponse<UploadResponse> = execute {
        httpClient.post("uploads") {
            parameter("filename", filename)
            contentType(ContentType.Text.Plain)
            setBody(content)
        }
    }

    internal suspend fun createRequest(request: SupportRequest): NetworkResponse<CreatedRequestResponse> =
        post("requests", CreateRequestBody(request))

    companion object {
        private const val BASE_URL = "https://blockstream.zendesk.com/api/v2/"
    }
}

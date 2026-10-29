// Tests which builds log the Zendesk HTTP traffic, whose bodies include the
// user's email and log files.

package com.blockstream.data.zendesk

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.blockstream.data.config.AppInfo
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ZendeskHttpClientTest {

    private val logged = mutableListOf<String>()

    @BeforeTest
    fun collectLogs() {
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                logged += message
            }
        })
    }

    @AfterTest
    fun restoreLogs() {
        Logger.setLogWriters(platformLogWriter())
    }

    private suspend fun createRequestLogs(appInfo: AppInfo): Boolean {
        val engine = MockEngine {
            respond(
                content = """{"request":{"id":35436,"status":"new","subject":"Bug report from green_android"}}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        logged.clear()
        ZendeskHttpClient(appInfo, engine).createRequest(
            SupportRequest(
                requester = Requester(email = "satoshi@example.com"),
                subject = "Bug report from green_android",
                comment = Comment(body = "The app crashes", uploads = emptyList()),
                customFields = emptyList()
            )
        )

        return logged.any { it.contains("https://blockstream.zendesk.com/api/v2/requests") }
    }

    @Test
    fun logs_requests_only_in_development_and_debug_builds() = runTest {
        mapOf(
            "development" to AppInfo(userAgent = "green_android", version = "5.7.1", isDebug = false, isDevelopment = true),
            "debug" to AppInfo(userAgent = "green_android", version = "5.7.1", isDebug = true, isDevelopment = false),
            "production" to AppInfo(userAgent = "green_android", version = "5.7.1", isDebug = false, isDevelopment = false)
        ).forEach { (build, appInfo) ->
            assertEquals(build != "production", createRequestLogs(appInfo), "$build build")
        }
    }
}

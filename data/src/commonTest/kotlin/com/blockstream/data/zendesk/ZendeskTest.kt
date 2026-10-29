// Tests support ticket submission against a Zendesk stand-in served by Ktor's
// MockEngine: the request fields, which log files are uploaded and how upload
// and request failures reach the caller.

package com.blockstream.data.zendesk

import com.blockstream.data.SupportType
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.SupportData
import com.blockstream.data.managers.SettingsManager
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.blockstream.utils.FileLogWriterRegistry
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable.Companion.COMBINED_LOG_QUALIFIER
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.SYSTEM
import okio.Sink
import okio.blackholeSink
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ZendeskTest {

    private class CapturedRequest(
        val method: HttpMethod,
        val url: Url,
        val contentType: ContentType?,
        val body: ByteArray
    ) {
        val json: JsonObject
            get() = Json.parseToJsonElement(body.decodeToString()).jsonObject
    }

    private val logsDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "zendesk-test-${Uuid.random()}"

    // The registry's writers truncate their files from a background coroutine, which
    // would race the log fixtures; send their writes nowhere so the fixtures survive.
    private val fileLogWriterRegistry = FileLogWriterRegistry(
        logsDir = logsDir,
        fileSystem = object : ForwardingFileSystem(FileSystem.SYSTEM) {
            override fun sink(file: Path, mustCreate: Boolean): Sink = blackholeSink()
        }
    )

    private val settingsManager = SettingsManager(
        settings = MapSettings(SettingsManager.KEY_COUNTLY_DEVICE_ID to DEVICE_ID),
        analyticsFeatureEnabled = false,
        lightningFeatureEnabled = false,
        storeRateEnabled = false
    )

    private val captured = mutableListOf<CapturedRequest>()

    private val uploads
        get() = captured.filter { it.url.encodedPath == "/api/v2/uploads" }

    private val createdRequest
        get() = captured.single { it.url.encodedPath == "/api/v2/requests" }.json.getValue("request").jsonObject

    @BeforeTest
    fun startKoinWithLogger() {
        loggedProblems.clear()
        startKoin {
            modules(
                module {
                    factory<Logger>(named(COMBINED_LOG_QUALIFIER)) { (tag: String, _: LogBucket) ->
                        Logger(config = StaticConfig(logWriterList = listOf(problemCollector)), tag = tag)
                    }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        FileSystem.SYSTEM.deleteRecursively(logsDir)
        assertEquals(emptyList(), loggedProblems, "Unexpected warnings or errors logged")
    }

    private fun writeLog(bucket: LogBucket, content: String) {
        FileSystem.SYSTEM.createDirectories(logsDir)
        FileSystem.SYSTEM.write(fileLogWriterRegistry.fileFor(bucket)) { writeUtf8(content) }
    }

    private fun zendesk(
        rejectUpload: (filename: String) -> Boolean = { false },
        requestReply: Pair<HttpStatusCode, String> = HttpStatusCode.Created to REQUEST_RESPONSE
    ): Zendesk {
        val engine = MockEngine { request ->
            captured += CapturedRequest(
                method = request.method,
                url = request.url,
                contentType = request.body.contentType?.withoutParameters(),
                body = request.body.toByteArray()
            )

            when (request.url.encodedPath) {
                "/api/v2/uploads" -> {
                    val filename = request.url.parameters["filename"]!!
                    if (rejectUpload(filename)) {
                        respond(
                            content = """{"error":"InternalError"}""",
                            status = HttpStatusCode.InternalServerError,
                            headers = JSON_HEADERS
                        )
                    } else {
                        respond(content = uploadResponse(filename), status = HttpStatusCode.Created, headers = JSON_HEADERS)
                    }
                }

                "/api/v2/requests" -> respond(content = requestReply.second, status = requestReply.first, headers = JSON_HEADERS)

                else -> respond(content = "", status = HttpStatusCode.NotFound)
            }
        }

        val appInfo = AppInfo(userAgent = "green_android", version = "5.7.1", isDebug = false, isDevelopment = false)
        return Zendesk(
            httpClient = ZendeskHttpClient(appInfo, engine),
            appInfo = appInfo,
            settingsManager = settingsManager,
            fileLogWriterRegistry = fileLogWriterRegistry
        )
    }

    private suspend fun Zendesk.submit(
        message: String = "The app crashes when I open my wallet",
        supportData: SupportData = SupportData()
    ) = submitNewTicket(
        type = SupportType.INCIDENT,
        subject = "Bug report from green_android",
        email = "satoshi@example.com",
        message = message,
        supportData = supportData
    )

    private fun JsonObject.customFields(): Map<Long, String> = getValue("custom_fields").jsonArray.associate {
        it.jsonObject.getValue("id").jsonPrimitive.long to it.jsonObject.getValue("value").jsonPrimitive.content
    }

    private fun JsonObject.uploadTokens(): List<String> =
        getValue("comment").jsonObject["uploads"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

    @Test
    fun creates_an_anonymous_request_with_the_support_fields() = runTest {
        zendesk().submit(
            supportData = SupportData(
                error = "Connection timed out",
                supportId = "support-123",
                zendeskSecurityPolicy = "singlesig__green_",
                zendeskHardwareWallet = "jade"
            )
        )

        val call = captured.single()
        assertEquals(HttpMethod.Post, call.method)
        assertEquals("https://blockstream.zendesk.com/api/v2/requests", call.url.toString())
        assertEquals(ContentType.Application.Json, call.contentType)

        val request = createdRequest
        assertEquals("satoshi@example.com", request.getValue("requester").jsonObject.getValue("email").jsonPrimitive.content)
        assertEquals("Bug report from green_android", request.getValue("subject").jsonPrimitive.content)
        assertEquals("The app crashes when I open my wallet", request.getValue("comment").jsonObject.getValue("body").jsonPrimitive.content)

        val fields = request.customFields()
        assertEquals(
            mapOf(
                42575138597145L to "incident",
                900003758323L to "green",
                900008231623L to operatingSystemName(),
                42657567831833L to operatingSystemVersion(),
                900009625166L to "5.7.1",
                42306364242073L to DEVICE_ID,
                23833728377881L to "support-123",
                900006375926L to "jade",
                6167739898649L to "singlesig__green_"
            ),
            fields - METADATA_FIELD
        )
        assertTrue(
            Regex("^Connection timed out\r\nTimestamp: \\d+\r\n$").matches(fields.getValue(METADATA_FIELD)),
            "Unexpected metadata: ${fields[METADATA_FIELD]}"
        )
    }

    @Test
    fun leaves_out_support_fields_without_a_value() = runTest {
        zendesk().submit(supportData = SupportData())

        val fields = createdRequest.customFields()
        assertEquals(
            setOf(42575138597145L, 900003758323L, 900008231623L, 42657567831833L, 900009625166L, 42306364242073L, METADATA_FIELD),
            fields.keys
        )
        assertTrue(
            Regex("^\r\nTimestamp: \\d+\r\n$").matches(fields.getValue(METADATA_FIELD)),
            "Unexpected metadata: ${fields[METADATA_FIELD]}"
        )
    }

    @Test
    fun sends_a_placeholder_body_when_the_message_is_blank() = runTest {
        zendesk().submit(message = "  ")

        assertEquals("{No Message}", createdRequest.getValue("comment").jsonObject.getValue("body").jsonPrimitive.content)
    }

    @Test
    fun does_not_upload_logs_unless_asked_to() = runTest {
        writeLog(LogBucket.App, "app log line\n")

        zendesk().submit(supportData = SupportData(attachLogs = false))

        assertEquals(listOf("/api/v2/requests"), captured.map { it.url.encodedPath })
        assertEquals(emptyList(), createdRequest.uploadTokens())
    }

    @Test
    fun uploads_each_non_empty_log_and_attaches_it_to_the_request() = runTest {
        writeLog(LogBucket.App, "app log line\n")
        writeLog(LogBucket.Gdk, " \n")
        writeLog(LogBucket.Lwk, "lwk log line\n")

        zendesk().submit(supportData = SupportData(attachLogs = true))

        assertEquals(2, uploads.size)
        uploads.zip(listOf("App" to "app log line\n", "Lwk" to "lwk log line\n")).forEach { (upload, expected) ->
            val (bucket, content) = expected
            assertEquals(HttpMethod.Post, upload.method)
            assertEquals("blockstream.zendesk.com", upload.url.host)
            assertTrue(
                Regex("^\\d{2}-\\d{2}-\\d{4}_\\d{2}-\\d{2}-\\d{2}_$bucket\\.log$").matches(upload.url.parameters["filename"]!!),
                "Unexpected filename: ${upload.url.parameters["filename"]}"
            )
            assertEquals(ContentType.Text.Plain, upload.contentType)
            assertEquals(content, upload.body.decodeToString())
        }
        assertEquals(listOf("token-App", "token-Lwk"), createdRequest.uploadTokens())
    }

    @Test
    fun uploads_only_the_end_of_a_log_larger_than_5_mb() = runTest {
        val limit = 5 * 1024 * 1024
        writeLog(LogBucket.App, "oldest" + "x".repeat(limit))

        zendesk().submit(supportData = SupportData(attachLogs = true))

        assertEquals("x".repeat(limit), uploads.single().body.decodeToString())
    }

    @Test
    fun sends_the_request_without_a_log_whose_upload_failed() = runTest {
        writeLog(LogBucket.App, "app log line\n")
        writeLog(LogBucket.Lwk, "lwk log line\n")

        zendesk(rejectUpload = { it.endsWith("_App.log") }).submit(supportData = SupportData(attachLogs = true))

        assertEquals(listOf("token-Lwk"), createdRequest.uploadTokens())
        assertEquals(listOf("App log upload failed: InternalError"), loggedProblems)
        loggedProblems.clear()
    }

    @Test
    fun succeeds_when_zendesk_suspends_the_request() = runTest {
        zendesk(requestReply = HttpStatusCode.Created to REQUEST_SUSPENDED_RESPONSE).submit()

        assertEquals("satoshi@example.com", createdRequest.getValue("requester").jsonObject.getValue("email").jsonPrimitive.content)
    }

    @Test
    fun fails_when_zendesk_rejects_the_request() = runTest {
        val error = assertFailsWith<Exception> {
            zendesk(requestReply = HttpStatusCode.UnprocessableEntity to REQUEST_INVALID_RESPONSE).submit()
        }

        assertTrue(error.message.orEmpty().contains("RecordInvalid"), "Unexpected error: ${error.message}")
    }

    companion object {
        // Loggable caches its Logger on first use, so one collector serves every test
        private val loggedProblems = mutableListOf<String>()
        private val problemCollector = object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                if (severity >= Severity.Warn) loggedProblems += message
            }
        }

        private const val DEVICE_ID = "3f2b7c1e-device"
        private const val METADATA_FIELD = 21409433258649L

        private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        private fun uploadResponse(filename: String): String {
            val token = "token-" + filename.substringAfterLast('_').removeSuffix(".log")
            val attachment = """{"id":498483,"file_name":"$filename","content_url":"https://blockstream.zendesk.com/attachments/token/$token/?name=$filename","content_type":"text/plain","size":13,"thumbnails":[]}"""
            return """{"upload":{"token":"$token","expires_at":"2026-10-29T12:00:00Z","attachments":[$attachment],"attachment":$attachment}}"""
        }

        private const val REQUEST_RESPONSE =
            """{"request":{"id":35436,"url":"https://blockstream.zendesk.com/api/v2/requests/35436.json","status":"new","subject":"Bug report from green_android","description":"The app crashes when I open my wallet","requester_id":1462,"created_at":"2026-10-29T10:00:00Z","custom_fields":[]}}"""

        // Zendesk's reply when it holds an anonymous request for an agent to recover
        private const val REQUEST_SUSPENDED_RESPONSE =
            """{"suspended_ticket":{"url":"https://blockstream.zendesk.com/api/v2/suspended_tickets/35437.json","id":35437,"author":{"id":1462,"name":"Satoshi","email":"satoshi@example.com"},"subject":"Bug report from green_android","content":"The app crashes when I open my wallet","cause":"Anonymous request","cause_id":36,"error_messages":null,"message_id":null,"ticket_id":null,"created_at":"2026-10-29T11:33:26Z","updated_at":"2026-10-29T11:33:26Z","via":{"channel":"api","source":{"from":{},"to":{},"rel":null}},"attachments":[],"recipient":null,"brand_id":900000266543}}"""

        private const val REQUEST_INVALID_RESPONSE =
            """{"error":"RecordInvalid","description":"Record validation errors","details":{"base":[{"description":"Email is invalid"}]}}"""
    }
}

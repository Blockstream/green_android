// Files support tickets as anonymous Zendesk requests, with the app's log files
// attached when the user chooses to share them.

package com.blockstream.data.zendesk

import com.blockstream.data.SupportType
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.SupportData
import com.blockstream.data.managers.SettingsManager
import com.blockstream.network.dataOrNull
import com.blockstream.network.dataOrThrow
import com.blockstream.network.error
import com.blockstream.utils.FileLogWriterRegistry
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.SYSTEM
import kotlin.time.Clock

class Zendesk(
    private val httpClient: ZendeskHttpClient,
    private val appInfo: AppInfo,
    private val settingsManager: SettingsManager,
    private val fileLogWriterRegistry: FileLogWriterRegistry
) {
    suspend fun submitNewTicket(
        type: SupportType,
        subject: String,
        email: String,
        message: String,
        supportData: SupportData
    ) {
        val uploads = if (supportData.attachLogs) uploadLogs() else emptyList()

        val request = SupportRequest(
            requester = Requester(email = email),
            subject = subject,
            comment = Comment(body = message.takeIf { it.isNotBlank() } ?: "{No Message}", uploads = uploads),
            customFields = listOfNotNull(
                CustomField(42575138597145, type.zendeskValue), // Type
                CustomField(900003758323, "green"), // Product
                CustomField(900008231623, operatingSystemName()), // OS
                CustomField(42657567831833, operatingSystemVersion()), // OS Version
                CustomField(900009625166, appInfo.version), // App Version
                CustomField(42306364242073, settingsManager.getCountlyDeviceId()), // Device ID
                supportData.supportId?.let { CustomField(23833728377881, it) }, // Support ID
                CustomField(21409433258649, metadata(supportData)), // Logs
                supportData.zendeskHardwareWallet?.let { CustomField(900006375926, it) }, // Hardware Wallet
                supportData.zendeskSecurityPolicy?.let { CustomField(6167739898649, it) } // Policy
            )
        )

        val created = httpClient.createRequest(request).dataOrThrow()
        logger.i {
            created.suspendedTicket?.let { "Zendesk suspended support request ${it.id}" }
                ?: "Created support request ${created.request?.id}"
        }
    }

    private fun metadata(supportData: SupportData): String = buildString {
        append(supportData.error ?: "")
        append("\r\n")
        append("Timestamp: ${Clock.System.now().epochSeconds}\r\n")
    }

    private suspend fun uploadLogs(): List<String> {
        val tokens = LogBucket.entries.mapNotNull { bucket ->
            val logs = readLog(bucket) ?: return@mapNotNull null
            val response = httpClient.uploadLog(
                filename = "${timestamp()}_${bucket.name}.log",
                content = logs.takeLast(MAX_LOG_LENGTH).encodeToByteArray()
            )
            response.dataOrNull()?.upload?.token.also {
                if (it == null) logger.e { "${bucket.name} log upload failed: ${response.error()}" }
            }
        }

        if (tokens.isEmpty()) {
            logger.e { "All log uploads failed or empty, sending without logs" }
        }

        return tokens
    }

    private suspend fun readLog(bucket: LogBucket): String? = withContext(Dispatchers.IO) {
        val file = fileLogWriterRegistry.fileFor(bucket)
        if (!FileSystem.SYSTEM.exists(file)) {
            return@withContext null
        }

        runCatching {
            FileSystem.SYSTEM.read(file) { readUtf8() }.takeIf { it.isNotBlank() }
        }.getOrElse { e ->
            logger.e { "${bucket.name} log read failed: ${e.message}" }
            null
        }
    }

    private fun timestamp(): String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).format(LocalDateTime.Format {
        day()
        char('-')
        monthNumber()
        char('-')
        year()
        char('_')
        hour()
        char('-')
        minute()
        char('-')
        second()
    })

    companion object : Loggable() {
        private const val MAX_LOG_LENGTH = 5 * 1024 * 1024
    }
}

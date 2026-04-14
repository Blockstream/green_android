package com.blockstream.gms

import android.content.Context
import android.os.Build
import com.blockstream.data.CountlyBase
import com.blockstream.data.SupportType
import com.blockstream.data.ZendeskSdk
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.SupportData
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.extensions.logException
import com.blockstream.utils.Loggable
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.RequestProvider
import zendesk.support.Support
import zendesk.support.UploadResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ZendeskSdkAndroid constructor(
    private val context: Context,
    private val appInfo: AppInfo,
    private val scope: ApplicationScope,
    private val countlyBase: CountlyBase,
    clientId: String
) : ZendeskSdk() {
    private val zendeskSdk = Zendesk.INSTANCE
    private val support = Support.INSTANCE

    init {
        zendeskSdk.init(context, URL, APPLICATION_ID, clientId)
        support.init(zendeskSdk)
    }

    override val isAvailable = true

    override val appVersion: String
        get() = appInfo.version

    private fun getMetadata(supportData: SupportData): String {
        return buildString {
            append(supportData.error ?: "")
            append("\r\n")

            val timestamp = System.currentTimeMillis() / 1000
            append("Timestamp: $timestamp\r\n")
        }
    }

    private suspend fun uploadLogFile(logs: String): UploadResponse = suspendCancellableCoroutine { continuation ->
        val provider = support.provider()?.uploadProvider()
        val timestamp = java.text.SimpleDateFormat(
            "dd-MM-yyyy_HH-mm-ss",
            java.util.Locale.getDefault()
        ).format(
            java.util.Date()
        )
        val filename = "android_logs_$timestamp.txt"

        if (provider == null) {
            continuation.resumeWithException(Exception("Zendesk UploadProvider is null"))
            return@suspendCancellableCoroutine
        }

        val tempFile = java.io.File(context.cacheDir, filename)
        continuation.invokeOnCancellation { tempFile.delete() }
        try {
            val maxLogBytes = 5 * 1024 * 1024 // 5 MB per log file
            val logData = logs.takeLast(maxLogBytes).toByteArray(Charsets.UTF_8)
            tempFile.writeBytes(logData)

            provider.uploadAttachment(tempFile.name, tempFile, "text/plain", object : ZendeskCallback<UploadResponse>() {
                override fun onSuccess(result: UploadResponse) {
                    tempFile.delete()
                    if (continuation.isActive) continuation.resume(result)
                }

                override fun onError(error: ErrorResponse) {
                    tempFile.delete()
                    if (continuation.isActive) continuation.resumeWithException(Exception("Zendesk upload error: ${error.reason}"))
                }
            })
        } catch (e: Exception) {
            tempFile.delete()
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    override suspend fun submitNewTicket(
        type: SupportType,
        subject: String,
        email: String,
        message: String,
        supportData: SupportData,
        autoRetry: Boolean
    ): Boolean {
        AnonymousIdentity.Builder().apply {
            if (email.isNotBlank()) {
                withEmailIdentifier(email)
            }
        }.build().also { identity ->
            zendeskSdk.setIdentity(identity)
        }
        val request = CreateRequest()

        if (!supportData.gdkLogs.isNullOrBlank()) {
            try {
                val uploadResponse = uploadLogFile(supportData.gdkLogs!!)
                val token = uploadResponse.token
                if (!token.isNullOrBlank()) {
                    request.attachments = listOf(token)
                } else {
                    logger.e { "Log upload returned empty token, sending without logs" }
                }
            } catch (e: Exception) {
                logger.e { "Log upload failed, sending without logs: ${e.message}" }
            }
        }

        request.tags = mutableListOf("android", "green")
        request.subject = subject
        request.description = message.takeIf { it.isNotBlank() } ?: "{No Message}"
        request.customFields = listOfNotNull(
            CustomField(42575138597145, type.zendeskValue), // Type
            CustomField(900003758323, "green"), // Product
            CustomField(900008231623, "android"), // OS
            CustomField(42657567831833, Build.VERSION.SDK_INT.toString()), // OS Version
            CustomField(900009625166, appVersion), // App Version
            CustomField(42306364242073, countlyBase.getDeviceId()), // Device ID
            supportData.supportId?.let { CustomField(23833728377881L, it) }, // Support ID
            CustomField(21409433258649L, getMetadata(supportData)), // Logs
            supportData.zendeskHardwareWallet?.let {
                CustomField(
                    900006375926L,
                    it
                )
            }, // Hardware Wallet
            supportData.zendeskSecurityPolicy?.let { CustomField(6167739898649L, it) } // Policy
        )

        return suspendCancellableCoroutine { continuation ->
            val provider: RequestProvider = support.provider()!!.requestProvider()

            provider.createRequest(request, object : ZendeskCallback<Request>() {
                override fun onSuccess(createRequest: Request) {
                    logger.i { "createRequest: ${request.id} Successfully created" }
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onError(errorResponse: ErrorResponse) {
                    logger.e { "createRequest: Error(${errorResponse.responseBody}) ... retry with delay" }

                    if (autoRetry) {
                        scope.launch(context = logException()) {
                            delay(1L.toDuration(DurationUnit.MINUTES))

                            submitNewTicket(
                                type = type,
                                subject = subject,
                                email = email,
                                message = message,
                                supportData = supportData,
                                autoRetry = false
                            ).let {
                                if (continuation.isActive) continuation.resume(it)
                            }
                        }
                    } else {
                        if (continuation.isActive) continuation.resumeWithException(Exception(errorResponse.reason))
                    }
                }
            })
        }
    }

    companion object : Loggable() {
        const val URL = "https://blockstream.zendesk.com"
        const val APPLICATION_ID = "12519480a4c4efbe883adc90777bb0f680186deece244799"
    }
}
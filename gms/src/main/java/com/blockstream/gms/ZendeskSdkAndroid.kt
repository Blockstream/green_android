package com.blockstream.gms

import android.content.Context
import android.os.Build
import com.blockstream.common.CountlyBase
import com.blockstream.common.SupportType
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.SupportData
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.logException
import com.blockstream.common.utils.Loggable
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.RequestProvider
import zendesk.support.Support
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ZendeskSdkAndroid constructor(
    context: Context,
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


    override suspend fun submitNewTicket(
        type: SupportType,
        subject: String,
        email: String,
        message: String,
        supportData: SupportData,
        autoRetry: Boolean
    ): Boolean = suspendCoroutine { continuation ->

        val request = CreateRequest()

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
            supportData.error?.let { CustomField(21409433258649L, it) }, // Logs
            supportData.zendeskHardwareWallet?.let {
                CustomField(
                    900006375926L,
                    it
                )
            }, // Hardware Wallet
            supportData.zendeskSecurityPolicy?.let { CustomField(6167739898649L, it) } // Policy
        )

        AnonymousIdentity.Builder().apply {
            if (email.isNotBlank()) {
                withEmailIdentifier(email)
            }
        }.build().also { identity ->
            zendeskSdk.setIdentity(identity)
        }

        val provider: RequestProvider = support.provider()!!.requestProvider()

        provider.createRequest(request, object : ZendeskCallback<Request>() {
            override fun onSuccess(createRequest: Request) {
                logger.i { "Success" }
                continuation.resume(true)
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
                            continuation.resume(it)
                        }
                    }
                } else {
                    continuation.resumeWithException(Exception(errorResponse.reason))
                }
            }
        })

    }

    companion object : Loggable() {
        const val URL = "https://blockstream.zendesk.com"
        const val APPLICATION_ID = "12519480a4c4efbe883adc90777bb0f680186deece244799"
    }
}
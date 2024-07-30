package com.blockstream.gms

import android.content.Context
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.logException
import com.russhwolf.settings.Settings
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogging
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.RequestProvider
import zendesk.support.Support
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ZendeskSdkAndroid(context: Context, val scope: ApplicationScope, clientId: String) : ZendeskSdk() {
    private val zendeskSdk = Zendesk.INSTANCE
    private val support = Support.INSTANCE

    init {
        zendeskSdk.init(context, URL, APPLICATION_ID, clientId)
        support.init(zendeskSdk)
    }

    override val isAvailable = true

    override fun submitNewTicket(
        subject: String?,
        email: String,
        message: String,
        errorReport: ErrorReport
    ) {
        val request = CreateRequest()

        request.tags = mutableListOf("android", "green")
        request.subject = subject
        request.description = message.takeIf { it.isNotBlank() } ?: "{No Message}"
        request.customFields = listOfNotNull(
            appVersion?.let { CustomField( 900009625166, it) }, // App Version
            CustomField( 21409433258649L, errorReport.error), // Logs
            errorReport.supportId?.let { CustomField(23833728377881L, it) }, // Support ID
            errorReport.zendeskHardwareWallet?.let { CustomField(900006375926L, it) }, // Hardware Wallet
            errorReport.zendeskSecurityPolicy?.let { CustomField(6167739898649L, it) } // Policy

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
                logger.info { "createRequest: Success" }
            }

            override fun onError(errorResponse: ErrorResponse) {
                logger.info { "createRequest: Error(${errorResponse.responseBody}) ... retry with delay"  }

                scope.launch(context = logException()) {
                    delay(1L.toDuration(DurationUnit.MINUTES))
                    submitNewTicket(
                        subject = subject,
                        email = email,
                        message = message,
                        errorReport = errorReport
                    )
                }
            }
        })
    }

    companion object: KLogging() {
        const val URL = "https://blockstream.zendesk.com"
        const val APPLICATION_ID = "12519480a4c4efbe883adc90777bb0f680186deece244799"
    }
}
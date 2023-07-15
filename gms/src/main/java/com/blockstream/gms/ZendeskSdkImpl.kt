package com.blockstream.gms

import android.content.Context
import com.blockstream.base.ZendeskSdk
import com.blockstream.common.data.ErrorReport
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import mu.KLogging
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.RequestProvider
import zendesk.support.Support

class ZendeskSdkImpl constructor(context: Context, clientId: String) : ZendeskSdk() {
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
            CustomField( 21409433258649, errorReport.error), // Logs
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
                logger.info { "Success" }
            }

            override fun onError(errorResponse: ErrorResponse) {
                logger.info { errorResponse.responseBody }
            }
        })
    }

    companion object: KLogging() {
        const val URL = "https://blockstream.zendesk.com"
        const val APPLICATION_ID = "12519480a4c4efbe883adc90777bb0f680186deece244799"
    }
}
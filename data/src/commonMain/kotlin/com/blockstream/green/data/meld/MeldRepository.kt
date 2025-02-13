package com.blockstream.green.data.meld

import co.touchlab.kermit.Logger
import com.blockstream.green.data.config.AppInfo
import com.blockstream.green.data.meld.data.CryptoQuoteRequest
import com.blockstream.green.data.meld.data.CryptoWidget
import com.blockstream.green.data.meld.data.CryptoWidgetRequest
import com.blockstream.green.data.meld.data.LimitsResponse
import com.blockstream.green.data.meld.data.QuotesResponse
import com.blockstream.green.data.meld.data.Resources
import com.blockstream.green.utils.Loggable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MeldRepository(client: HttpClient, private val appInfo: AppInfo) {
    private val client = client.config {
        if (appInfo.isDevelopmentOrDebug) {
            install(Logging) {
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        Logger.d { message }
                    }
                }
                level = LogLevel.BODY
            }
        }

        install(HttpTimeout) {
            this.requestTimeoutMillis = 45_000
        }

        defaultRequest {
            // url(MELD_PRODUCTION.takeIf { appInfo.isProduction } ?: MELD_SANDBOX)
            url(MELD_PRODUCTION)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun createCryptoQuote(cryptoQuote: CryptoQuoteRequest): QuotesResponse {
        return client.post(Resources.Payments.Crypto.Quote()) {
            setBody(cryptoQuote)
        }.body()
    }

    suspend fun createCryptoWidget(widgetRequest: CryptoWidgetRequest): CryptoWidget {
        return client.post(Resources.Crypto.Session.Widget()) {
            setBody(widgetRequest)
        }.body()
    }

    suspend fun getCryptoLimits(fiatCurrency: String = "USD"): LimitsResponse {
        return client.get(Resources.Payments.Crypto.Limits(fiatCurrency = fiatCurrency))
            .body<List<LimitsResponse>>().first()
    }

    suspend fun getTransactions(externalCustomerId: String): LimitsResponse {
        return client.get(Resources.Payments.Transactions(externalCustomerIds = externalCustomerId)) {
        }.body()
    }

    companion object : Loggable() {
        private const val MELD_PRODUCTION = "https://ramps.blockstream.com"
        private const val MELD_SANDBOX = "https://ramps.sandbox.blockstream.com"
    }
}
package com.blockstream.green.data.meld

import com.blockstream.data.config.AppInfo
import com.blockstream.data.meld.MeldHttpClient
import com.blockstream.data.meld.MeldRepository
import com.blockstream.data.meld.data.CryptoQuoteRequest
import com.blockstream.data.meld.datasource.MeldLocalDataSource
import com.blockstream.data.meld.datasource.MeldRemoteDataSource
import com.blockstream.network.NetworkResponse
import com.blockstream.network.dataOrThrow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercises the real HTTP client with an in-memory transport, without creating live sessions. */
class MeldRepositoryTest {
    @Test
    fun `Request CryptoQuoteRequest`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://ramps.blockstream.com/payments/crypto/quote", request.url.toString())
            assertEquals("application/json", request.body.contentType?.toString())
            assertEquals(
                Json.parseToJsonElement("""{"countryCode":"US","sourceAmount":"200","sourceCurrencyCode":"USD","destinationCurrencyCode":"BTC"}"""),
                Json.parseToJsonElement(request.body.toByteArray().decodeToString()),
            )
            respond(quoteResponse, HttpStatusCode.OK, jsonHeaders)
        }
        withRepository(engine) { repository ->
            val quote = repository.createCryptoQuote(CryptoQuoteRequest()).dataOrThrow().quotes!!.single()
            assertEquals("BTC", quote.destinationCurrencyCode)
            assertEquals("0.002", quote.destinationAmount)
            assertEquals("TEST_PROVIDER", quote.serviceProvider)
        }
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `Request CryptoWidgetRequest`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("ramps.blockstream.com", request.url.host)
            when (request.url.encodedPath) {
                "/payments/crypto/quote" -> respond(quoteResponse, HttpStatusCode.OK, jsonHeaders)
                "/crypto/session/widget" -> {
                    assertEquals(
                        Json.parseToJsonElement("""{"sessionType":"BUY","externalCustomerId":"test-customer","sessionData":{"countryCode":"US","sourceAmount":"200","sourceCurrencyCode":"USD","destinationCurrencyCode":"BTC","walletAddress":"test-wallet-address","serviceProvider":"TEST_PROVIDER","redirectUrl":"https://green-webhooks.blockstream.com/thank-you"}}"""),
                        Json.parseToJsonElement(request.body.toByteArray().decodeToString()),
                    )
                    respond(
                        """{"id":"test-session","customerId":"test-customer","widgetUrl":"https://example.test/widget","token":"test-token"}""",
                        HttpStatusCode.OK, jsonHeaders,
                    )
                }
                else -> error("Unexpected request: ${request.url}")
            }
        }
        withRepository(engine) { repository ->
            val request = repository.createCryptoQuote(CryptoQuoteRequest()).dataOrThrow().quotes!!.single()
                .toCryptoWidgetRequest("test-wallet-address", "test-customer")
            val widget = repository.createCryptoWidget(request).dataOrThrow()
            assertEquals("test-session", widget.id)
            assertEquals("https://example.test/widget", widget.widgetUrl)
        }
        assertEquals(2, engine.requestHistory.size)
    }

    @Test
    fun `Request CryptoLimitsRequest`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/payments/crypto/limits", request.url.encodedPath)
            assertEquals("EUR", request.url.parameters["fiatCurrency"])
            respond(
                """[{"currencyCode":"EUR","defaultAmount":200,"minAmount":20,"maxAmount":1000}]""",
                HttpStatusCode.OK, jsonHeaders,
            )
        }
        withRepository(engine) { repository ->
            val limit = repository.getCryptoLimits("EUR").dataOrThrow().single()
            assertEquals("EUR", limit.currencyCode)
            assertEquals(20.0, limit.minAmount)
            assertEquals(1000.0, limit.maxAmount)
        }
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun `Forbidden response remains an error`() = runTest {
        val engine = MockEngine {
            respond("""{"message":"Forbidden"}""", HttpStatusCode.Forbidden, jsonHeaders)
        }
        withRepository(engine) { repository ->
            assertEquals(NetworkResponse.Error(403, "Forbidden"), repository.getCryptoLimits("EUR"))
        }
    }

    @Test
    fun `Malformed successful response remains an error`() = runTest {
        val engine = MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders) }
        withRepository(engine) { repository ->
            assertTrue(repository.getCryptoLimits("EUR") is NetworkResponse.Error)
        }
    }

    private suspend fun withRepository(engine: MockEngine, block: suspend (MeldRepository) -> Unit) {
        val client = MeldHttpClient(AppInfo("test", "0.0.0", isDebug = false, isDevelopment = false, isTest = true), engine)
        try {
            block(MeldRepository(MeldRemoteDataSource(client), MeldLocalDataSource()))
        } finally {
            client.httpClient.close()
            engine.close()
        }
    }

    companion object {
        private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
        private const val quoteResponse = """{"quotes":[{"transactionType":"BUY","sourceAmount":"200","sourceAmountWithoutFees":"190","fiatAmountWithoutFees":"190","sourceCurrencyCode":"USD","countryCode":"US","totalFee":"10","transactionFee":"10","destinationAmount":"0.002","destinationCurrencyCode":"BTC","exchangeRate":"95000","paymentMethodType":"CARD","customerScore":"1","serviceProvider":"TEST_PROVIDER"}]}"""
    }
}

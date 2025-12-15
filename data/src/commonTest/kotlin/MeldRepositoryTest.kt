package com.blockstream.green.data.meld

import com.blockstream.data.config.AppInfo
import com.blockstream.data.dataModule
import com.blockstream.utils.Loggable
import com.blockstream.network.dataOrThrow
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*

class MeldRepositoryTest : KoinTest {

    companion object : Loggable()

    private val meldRepository: com.blockstream.data.meld.MeldRepository by inject()

    @BeforeTest
    fun beforeTest() {
        startKoin {
            modules(
                module {
                    single {
                        AppInfo(userAgent = "test", "0.0.0", isDebug = false, isDevelopment = false, isTest = true)
                    }
                },
                dataModule
            )
        }
    }

    @AfterTest
    fun afterTest() {
        stopKoin()
    }

    @Test
    fun `Request CryptoQuoteRequest`() = runTest {
        meldRepository.createCryptoQuote(_root_ide_package_.com.blockstream.data.meld.data.CryptoQuoteRequest()).also {
            logger.d { "$it" }
            assertNotNull(it.dataOrThrow().quotes)
        }
    }

    @Test
    fun `Request CryptoWidgetRequest`() = runTest {
        meldRepository.createCryptoQuote(_root_ide_package_.com.blockstream.data.meld.data.CryptoQuoteRequest())
            .dataOrThrow().quotes!!.first().let {
            it.toCryptoWidgetRequest("bc1qcr8ktl3nzwh8xm88225ysynt5zsdydae26thrg")
        }.also {
            meldRepository.createCryptoWidget(it).also {
                logger.d { "$it" }
                assertNotNull(it.dataOrThrow().widgetUrl)
            }
        }
    }

    @Test
    fun `Request CryptoLimitsRequest`() = runTest {
        meldRepository.getCryptoLimits(fiatCurrency = "EUR").also {
            assertNotEquals(0.0, it.dataOrThrow().first().maxAmount)
        }
    }

}

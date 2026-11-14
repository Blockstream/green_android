package com.blockstream.data.data

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable.Companion.COMBINED_LOG_QUALIFIER
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromoParserTest {

    @BeforeTest
    fun beforeTest() {
        startKoin {
            modules(
                module {
                    factory<Logger>(named(COMBINED_LOG_QUALIFIER)) { (tag: String, _: LogBucket) ->
                        Logger(config = StaticConfig(logWriterList = emptyList()), tag = tag)
                    }
                }
            )
        }
    }

    @AfterTest
    fun afterTest() {
        stopKoin()
    }

    @Test
    fun malformedElementsDoNotSuppressValidSiblings() {
        val result = parsePromosV2(
            Json.parseToJsonElement(
                """
                [
                  {"id":"first","title":"First","description":"Description","cta":{"label":"Open","url":"https://example.com/first"}},
                  {"id":"broken","description":"Missing title","cta":{"label":"Open","url":"https://example.com/broken"}},
                  {"id":"third","title":"Third","description":"Description","cta":{"label":"Open","url":"https://example.com/third"}}
                ]
                """
            ).jsonArray
        )

        assertEquals(listOf("first", "third"), result.map { it.id })
    }

    @Test
    fun duplicateIdsKeepTheFirstCampaign() {
        val result = parsePromosV2(
            Json.parseToJsonElement(
                """
                [
                  {"id":"same","title":"First","description":"Description","cta":{"label":"Open","url":"https://example.com/first"}},
                  {"id":"same","title":"Second","description":"Description","cta":{"label":"Open","url":"https://example.com/second"}}
                ]
                """
            ).jsonArray
        )

        assertEquals(1, result.size)
        assertEquals("First", result.single().title)
    }

    @Test
    fun invalidImageKeepsOtherwiseValidCampaign() {
        val result = parsePromosV2(
            Json.parseToJsonElement(
                """
                [{"id":"no-image","title":"Title","description":"Description","cta":{"label":"Open","url":"https://example.com"},"imageUrl":"not-an-https-url"}]
                """
            ).jsonArray
        )

        assertTrue(result.single().isValid())
        assertFalse(result.single().hasValidImageUrl)
    }
}

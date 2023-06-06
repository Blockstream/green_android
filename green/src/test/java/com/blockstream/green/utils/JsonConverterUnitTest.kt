package com.blockstream.green.utils

import com.blockstream.common.gdk.data.Pricing
import com.blockstream.common.gdk.data.Settings
import com.blockstream.green.data.Denomination
import com.blockstream.green.gdk.GdkSession
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class JsonConverterUnitTest {

    @Mock
    private lateinit var session: GdkSession

    private val dotLocale = Locale.US
    private val commaLocale = Locale.ITALIAN

    private fun initMock(fiat: String) {
        val settings: Settings = mock()

        whenever(settings.pricing).thenReturn(
            Pricing(
            fiat, fiat
        )
        )
        whenever(session.getSettings(anyOrNull())).thenReturn(settings)
    }

    @Test
    fun test_valuesInBTC() {
        val tests = mapOf(
            "123" to "123",
            "123.1" to "123.1",
            "123.123" to "123.123",
            "12356789.123" to "12356789.123",
            "12356789.123" to "12 356 789.123"
        )

        // Dot - US Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace(' ', ','),
                    locale = dotLocale,
                ).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    locale = commaLocale,
                ).amount
            )
        }
    }

    @Test
    fun test_valuesInSat() {
        val tests = mapOf(
            "123" to "123",
            "123" to "123.1",
            "123456789" to "123456789",
            "123456789" to "123456789.123",
            "123456789" to "123 456 789.123",
        )

        // Dot - US Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(session, test.value.replace(' ', ','), locale = dotLocale, denomination = Denomination.SATOSHI).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    locale = commaLocale,
                    denomination = Denomination.SATOSHI
                ).amount
            )
        }
    }

    @Test
    fun test_valuesInFiat() {
        initMock("EUR")

        val tests = mapOf(
            "123.00" to "123",
            "123.10" to "123.10",
            "123.12" to "123.123",
            "123.13" to "123.129",
            "123456789.00" to "123456789",
            "123456789.00" to "123 456 789",
            "123456789.12" to "123 456 789.123",
        )

        // Dot - US Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(session, test.value.replace(' ', ','), denomination = Denomination.fiat(session), locale = dotLocale).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    denomination = Denomination.fiat(session),
                    locale = commaLocale
                ).amount
            )
        }
    }

    @Test
    fun test_invalid_inputs() {
        Assert.assertEquals("", UserInput.parseUserInputSafe(session, null).amount)
        Assert.assertEquals("", UserInput.parseUserInputSafe(session, "abc").amount)
        Assert.assertEquals("", UserInput.parseUserInputSafe(session, "123abc").amount)
    }

    @Test
    fun test_invalid_inputs_throws() {
        Assert.assertThrows(Exception::class.java) {
            UserInput.parseUserInput(session, null)
        }
        Assert.assertThrows(Exception::class.java) {
            UserInput.parseUserInput(session, "abc")
        }
        Assert.assertThrows(Exception::class.java) {
            UserInput.parseUserInput(session, "123abc")
        }
    }


    @Test
    fun test_grouping() {
        Assert.assertEquals("123123.1", UserInput.parseUserInputSafe(session, "123,123.10", locale =  Locale.US).amount)
        Assert.assertEquals("123123.1", UserInput.parseUserInputSafe(session, "123.123,10", locale =  Locale.ITALIAN).amount)
    }
}
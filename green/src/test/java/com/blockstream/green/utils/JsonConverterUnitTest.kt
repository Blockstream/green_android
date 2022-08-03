package com.blockstream.green.utils

import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.Settings
import com.blockstream.green.gdk.GdkSession
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class JsonConverterUnitTest {

    @Mock
    private lateinit var network: Network

    @Mock
    private lateinit var session: GdkSession

    private val dotLocale = Locale.US
    private val commaLocale = Locale.ITALIAN

    private fun initMock(unit: String) {
        val settings: Settings = mock()

        whenever(settings.unit).thenReturn(unit)
        whenever(session.getSettings(any())).thenReturn(settings)
    }

    @Test
    fun test_valuesInBTC() {
        initMock("BTC")

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
                    isFiat = false,
                    locale = dotLocale,
                    networkForTest = network,
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
                    isFiat = false,
                    locale = commaLocale,
                    networkForTest = network,
                ).amount
            )
        }
    }

    @Test
    fun test_valuesInSat() {
        initMock("sat")

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
                UserInput.parseUserInput(session, test.value.replace(' ', ','), isFiat = false, locale = dotLocale, networkForTest = network).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    isFiat = false,
                    locale = commaLocale,
                    networkForTest = network
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
                UserInput.parseUserInput(session, test.value.replace(' ', ','), isFiat = true, locale = dotLocale, networkForTest = network).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    isFiat = true,
                    locale = commaLocale,
                    networkForTest = network
                ).amount
            )
        }
    }

    @Test
    fun test_invalid_inputs() {
        initMock("BTC")

        Assert.assertEquals("", UserInput.parseUserInputSafe(session, null, networkForTest = network).amount)
        Assert.assertEquals("", UserInput.parseUserInputSafe(session, "abc", networkForTest = network).amount)
        Assert.assertEquals("", UserInput.parseUserInputSafe(session, "123abc", networkForTest = network).amount)
    }

    @Test
    fun test_invalid_inputs_throws() {
        initMock("BTC")

        Assert.assertThrows(Exception::class.java) {
            UserInput.parseUserInput(session, null, networkForTest = network)
        }
        Assert.assertThrows(Exception::class.java) {
            UserInput.parseUserInput(session, "abc", networkForTest = network)
        }
        Assert.assertThrows(Exception::class.java) {
            UserInput.parseUserInput(session, "123abc", networkForTest = network)
        }
    }


    @Test
    fun test_grouping() {
        Assert.assertEquals("123123.1", UserInput.parseUserInputSafe(session, "123,123.10", locale =  Locale.US, networkForTest = network).amount)
        Assert.assertEquals("123123.1", UserInput.parseUserInputSafe(session, "123.123,10", locale =  Locale.ITALIAN, networkForTest = network).amount)
    }
}
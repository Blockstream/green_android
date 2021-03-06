package com.blockstream.green.utils

import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.Settings
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.settings.SettingsManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.text.DecimalFormat
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class UserInputUnitTests {

    @Mock
    private lateinit var session: GreenSession

    private val dotLocale = Locale.US
    private val commaLocale = Locale.ITALIAN

    private fun initMock(unit: String) {
        val settings: Settings = mock()

        whenever(settings.unit).thenReturn(unit)
        whenever(session.getSettings()).thenReturn(settings)
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
                    false,
                    dotLocale
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
                    false,
                    commaLocale
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
                UserInput.parseUserInput(session, test.value.replace(' ', ','), false, dotLocale).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    false,
                    commaLocale
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
                UserInput.parseUserInput(session, test.value.replace(' ', ','), true, dotLocale).amount
            )
        }

        // Comma - IT Locale
        for (test in tests) {
            Assert.assertEquals(
                test.key,
                UserInput.parseUserInput(
                    session,
                    test.value.replace('.', ',').replace(' ', '.'),
                    true,
                    commaLocale
                ).amount
            )
        }
    }
}
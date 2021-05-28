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

@RunWith(MockitoJUnitRunner::class)
class UserInputUnitTests() {

    @Mock
    private lateinit var session: GreenSession

    private fun initMock(unit: String) {
        val settings: Settings = mock()

        whenever(settings.unit).thenReturn(unit)
        whenever(session.getSettings()).thenReturn(settings)
    }

    @Test
    fun test_valuesInBTC() {
        initMock("BTC")

        Assert.assertEquals("123", UserInput.parseUserInput(session, "123", false).amount)
        Assert.assertEquals("123.1", UserInput.parseUserInput(session, "123.1", false).amount)
        Assert.assertEquals("123.123", UserInput.parseUserInput(session, "123.123", false).amount)

        Assert.assertEquals("12356789.123", UserInput.parseUserInput(session, "12356789.123", false).amount)
    }

    @Test
    fun test_valuesInSat() {
        initMock("sat")

        Assert.assertEquals("123", UserInput.parseUserInput(session, "123", false).amount)
        Assert.assertEquals("123", UserInput.parseUserInput(session, "123.1", false).amount)
        Assert.assertEquals("123", UserInput.parseUserInput(session, "123.123", false).amount)

        Assert.assertEquals("123456789", UserInput.parseUserInput(session, "123456789", false).amount)
    }

    @Test
    fun test_valuesInFiat() {
        initMock("EUR")

        Assert.assertEquals("123.00", UserInput.parseUserInput(session, "123.00", true).amount)
        Assert.assertEquals("123.10", UserInput.parseUserInput(session, "123.10", true).amount)
        Assert.assertEquals("123.12", UserInput.parseUserInput(session, "123.123", true).amount)
        Assert.assertEquals("123.13", UserInput.parseUserInput(session, "123.129", true).amount)

        Assert.assertEquals("123456789.00", UserInput.parseUserInput(session, "123456789", true).amount)
    }
}
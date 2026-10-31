// Tests AppConfig.default: the Greenlight credentials the build passes in (empty when the build has none)
// decide whether the Lightning feature stays enabled. Runs without Koin, as the apps build AppConfig
// before they start Koin.
package com.blockstream.data.data

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class CapturedLog : LogWriter() {
    val messages = mutableListOf<String>()

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        messages += message
    }
}

class AppConfigTest {

    private val log = CapturedLog()

    @BeforeTest
    fun beforeTest() {
        Logger.setLogWriters(log)
    }

    @AfterTest
    fun afterTest() {
        Logger.setLogWriters(platformLogWriter())
    }

    private fun config(greenlightCert: String, greenlightKey: String) = AppConfig.default(
        isDebug = false,
        filesDir = "files",
        cacheDir = "cache",
        analyticsFeatureEnabled = false,
        lightningFeatureEnabled = true,
        storeRateEnabled = false,
        greenlightCert = greenlightCert,
        greenlightKey = greenlightKey
    )

    @Test
    fun credentialsKeepLightningEnabled() {
        val config = config(greenlightCert = "Y2VydA==", greenlightKey = "a2V5")

        assertEquals("Y2VydA==", config.greenlightCert)
        assertEquals("a2V5", config.greenlightKey)
        assertTrue(config.lightningFeatureEnabled)
        assertEquals(emptyList(), log.messages)
    }

    @Test
    fun emptyCredentialsTurnLightningOff() {
        val config = config(greenlightCert = "", greenlightKey = "")

        assertNull(config.greenlightCert)
        assertNull(config.greenlightKey)
        assertFalse(config.lightningFeatureEnabled)
        assertEquals(listOf("Lightning Feature turned off"), log.messages)
    }

    @Test
    fun certificateWithoutKeyTurnsLightningOff() {
        val config = config(greenlightCert = "Y2VydA==", greenlightKey = "")

        assertNull(config.greenlightKey)
        assertFalse(config.lightningFeatureEnabled)
        assertEquals(listOf("Lightning Feature turned off"), log.messages)
    }
}

/**
 * Tests for the routing of the personal Electrum server between GDK's electrum_url and
 * electrum_onion_url connection parameters. GDK prefers electrum_onion_url when use_tor
 * is set and always dials it without TLS, so only actual onion services may be placed
 * there; clearnet servers must stay in electrum_url with an explicit empty onion override
 * so GDK does not fall back to the network's default onion server.
 */
package com.blockstream.data.gdk.params

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionParamsTest {

    @Test
    fun noPersonalServerLeavesOnionUrlUnset() {
        assertEquals(null, ConnectionParams.electrumOnionUrl(electrumUrl = null, useTor = true))
        assertEquals(null, ConnectionParams.electrumOnionUrl(electrumUrl = null, useTor = false))
    }

    @Test
    fun torDisabledLeavesOnionUrlUnset() {
        assertEquals(null, ConnectionParams.electrumOnionUrl(electrumUrl = "electrum.example.com:50002", useTor = false))
        assertEquals(null, ConnectionParams.electrumOnionUrl(electrumUrl = "abcdef.onion:50001", useTor = false))
    }

    @Test
    fun onionServerWithTorIsUsedAsOnionEndpoint() {
        assertEquals(
            "abcdef.onion:50001",
            ConnectionParams.electrumOnionUrl(electrumUrl = "abcdef.onion:50001", useTor = true)
        )
        assertEquals(
            "abcdef.onion",
            ConnectionParams.electrumOnionUrl(electrumUrl = "abcdef.onion", useTor = true)
        )
    }

    @Test
    fun clearnetServerWithTorClearsDefaultOnionEndpoint() {
        assertEquals(
            "",
            ConnectionParams.electrumOnionUrl(electrumUrl = "electrum.example.com:50002", useTor = true)
        )
    }

    @Test
    fun onionDetectionMatchesHostOnlyNotSubstrings() {
        assertEquals(
            "",
            ConnectionParams.electrumOnionUrl(electrumUrl = "myonion.example.com:50002", useTor = true)
        )
        assertEquals(
            "",
            ConnectionParams.electrumOnionUrl(electrumUrl = "example.onion.com:50002", useTor = true)
        )
    }

    @Test
    fun emptyOnionUrlIsSerializedToOverrideNetworkDefault() {
        val json = connectionParams(electrumOnionUrl = "").toJson()
        assertTrue(json.contains("\"electrum_onion_url\":\"\""), json)
    }

    @Test
    fun nullOnionUrlIsOmittedToKeepNetworkDefault() {
        val json = connectionParams(electrumOnionUrl = null).toJson()
        assertFalse(json.contains("electrum_onion_url"), json)
    }

    private fun connectionParams(electrumOnionUrl: String?) = ConnectionParams(
        networkName = "electrum-mainnet",
        useTor = true,
        userAgent = "green",
        proxy = "",
        electrumUrl = "electrum.example.com:50002",
        electrumOnionUrl = electrumOnionUrl
    )
}

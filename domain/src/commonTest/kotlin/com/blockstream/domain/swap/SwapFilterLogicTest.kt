package com.blockstream.domain.swap

import com.blockstream.data.gdk.data.Network
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwapFilterLogicTest {

    private val btcNetwork = Network(
        network = Network.ElectrumMainnet,
        name = "Bitcoin",
        isMainnet = true,
        isLiquid = false,
        isDevelopment = false,
        policyAsset = "btc"
    )

    private val liquidNetwork = Network(
        network = Network.ElectrumLiquid,
        name = "Liquid",
        isMainnet = true,
        isLiquid = true,
        isDevelopment = false,
        policyAsset = "6f0279e9ed041c3d710a9f57d0c02928416460c4b722ae3457a11eec381c526d"
    )

    private val lightningNetwork = Network(
        network = Network.LightningMainnet,
        name = "Lightning",
        isMainnet = true,
        isLiquid = false,
        isDevelopment = false,
        isLightning = true
    )

    // Replicates the filter from GetSwappableAccountsUseCase
    private fun isSwapAllowed(from: Network, to: Network): Boolean {
        return when {
            from.isSameNetwork(to) -> false
            from.isLightning || to.isLightning -> false
            else -> true
        }
    }

    @Test
    fun btcToLiquid_allowed() {
        assertTrue(isSwapAllowed(btcNetwork, liquidNetwork))
    }

    @Test
    fun liquidToBtc_allowed() {
        assertTrue(isSwapAllowed(liquidNetwork, btcNetwork))
    }

    @Test
    fun sameNetwork_blocked() {
        assertFalse(isSwapAllowed(btcNetwork, btcNetwork))
        assertFalse(isSwapAllowed(liquidNetwork, liquidNetwork))
        assertFalse(isSwapAllowed(lightningNetwork, lightningNetwork))
    }

    @Test
    fun lightningToBtc_blocked() {
        assertFalse(isSwapAllowed(lightningNetwork, btcNetwork))
    }

    @Test
    fun btcToLightning_blocked() {
        assertFalse(isSwapAllowed(btcNetwork, lightningNetwork))
    }

    @Test
    fun lightningToLiquid_blocked() {
        assertFalse(isSwapAllowed(lightningNetwork, liquidNetwork))
    }

    @Test
    fun liquidToLightning_blocked() {
        assertFalse(isSwapAllowed(liquidNetwork, lightningNetwork))
    }
}

package com.blockstream.domain.lightning

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.lightning.GreenlightSdk
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.data.utils.toHex
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GateLightningActivationUseCaseTest {

    private val nodeId = ByteArray(33) { it.toByte() }
    private val nodeIdHex = nodeId.toHex()

    private val session = mockk<GdkSession>()
    private val walletSettingsManager = mockk<WalletSettingsManager>(relaxUnitFun = true)
    private val isLightningAvailableUseCase = mockk<IsLightningAvailableUseCase>()

    private val useCase = GateLightningActivationUseCase(walletSettingsManager, isLightningAvailableUseCase)

    @BeforeTest
    fun setUp() {
        startKoin { }
        mockkObject(GreenlightSdk.Companion)
        coEvery { GreenlightSdk.nodeId(MNEMONIC) } returns nodeId
        coEvery { walletSettingsManager.getLightningAvailableNodeId(WALLET_ID) } returns null
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(GreenlightSdk.Companion)
        stopKoin()
    }

    @Test
    fun aCachedTrueSkipsTheCall() = runTest {
        coEvery { walletSettingsManager.getLightningAvailableNodeId(WALLET_ID) } returns nodeIdHex.lowercase()

        assertEquals(nodeIdHex, useCase(session, wallet(), MNEMONIC))

        coVerify(exactly = 0) { isLightningAvailableUseCase(any(), any()) }
    }

    @Test
    fun aCacheForAnotherNodeIsIgnored() = runTest {
        coEvery { walletSettingsManager.getLightningAvailableNodeId(WALLET_ID) } returns "02" + "00".repeat(32)
        coEvery { isLightningAvailableUseCase(session, nodeId) } returns true

        useCase(session, wallet(), MNEMONIC)

        coVerify(exactly = 1) { isLightningAvailableUseCase(session, nodeId) }
    }

    @Test
    fun trueIsCached() = runTest {
        coEvery { isLightningAvailableUseCase(session, nodeId) } returns true

        assertEquals(nodeIdHex, useCase(session, wallet(), MNEMONIC))

        coVerify { walletSettingsManager.setLightningAvailableNodeId(WALLET_ID, nodeIdHex) }
    }

    @Test
    fun trueIsNotCachedForAnEphemeralWallet() = runTest {
        coEvery { isLightningAvailableUseCase(session, nodeId) } returns true

        useCase(session, wallet(isEphemeral = true), MNEMONIC)

        coVerify(exactly = 0) { walletSettingsManager.setLightningAvailableNodeId(any(), any()) }
    }

    @Test
    fun falseThrowsAtCapacity() = runTest {
        coEvery { isLightningAvailableUseCase(session, nodeId) } returns false

        assertFailsWith<LightningAtCapacityException> { useCase(session, wallet(), MNEMONIC) }

        coVerify(exactly = 0) { walletSettingsManager.setLightningAvailableNodeId(any(), any()) }
    }

    @Test
    fun nullThrowsUnknown() = runTest {
        coEvery { isLightningAvailableUseCase(session, nodeId) } returns null

        assertFailsWith<LightningAvailabilityUnknownException> { useCase(session, wallet(), MNEMONIC) }

        coVerify(exactly = 0) { walletSettingsManager.setLightningAvailableNodeId(any(), any()) }
    }

    @Test
    fun aFailedDerivationThrowsUnknown() = runTest {
        coEvery { GreenlightSdk.nodeId(MNEMONIC) } throws Exception("invalid mnemonic")

        assertFailsWith<LightningAvailabilityUnknownException> { useCase(session, wallet(), MNEMONIC) }

        coVerify(exactly = 0) { isLightningAvailableUseCase(any(), any()) }
    }

    private fun wallet(isEphemeral: Boolean = false) = mockk<GreenWallet> {
        every { id } returns WALLET_ID
        every { this@mockk.isEphemeral } returns isEphemeral
    }

    companion object {
        private const val WALLET_ID = "wallet"
        private const val MNEMONIC = "test mnemonic"
    }
}

package com.blockstream.domain.wallet

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Assets
import com.blockstream.domain.base.firstSettled
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val POLICY_ASSET = "btc"

class GetWalletAssetsUseCaseTest {

    private val lightningLoading = MutableStateFlow(false)
    private val accountsFlow = MutableStateFlow(listOf(mockk<Account>(relaxed = true)))

    private var balance = 0L

    private fun session(): GdkSession = mockk(relaxed = true) {
        every { isConnected } returns true
        every { isLightningLoading } returns lightningLoading
        every { accounts } returns accountsFlow
        every { networkBackendsStateFlow } returns MutableStateFlow(emptyMap())
        every { networkBackends } returns emptyMap()
        coEvery {
            getBalance(account = any(), confirmations = any(), cacheAssets = any())
        } answers { Assets(mapOf(POLICY_ASSET to balance)) }
    }

    @Test
    fun settlesWhileLightningIsStillConnecting() = runBlocking {
        lightningLoading.value = true

        val useCase = GetWalletAssetsUseCase(session())

        withTimeout(5_000) { useCase.firstSettled() }

        assertTrue(useCase.getCurrent().isSuccess(), "expected Success, was ${useCase.getCurrent()}")
    }

    @Test
    fun recomputesWhenLightningSettlesBeforeAnythingSubscribes() = runBlocking {
        lightningLoading.value = true
        val useCase = GetWalletAssetsUseCase(session())

        balance = 1L
        useCase.invoke(Unit)
        assertEquals(1L, useCase.getCurrent().data()?.policyAsset)

        balance = 2L
        lightningLoading.value = false

        val collector = launch { useCase.observe().collect { } }

        withTimeout(5_000) { useCase.get().first { it.data()?.policyAsset == 2L } }
        collector.cancel()
    }

    @Test
    fun recomputesWhenTheRestorePathOpensAndClosesTheWindow() = runBlocking {
        lightningLoading.value = false
        val useCase = GetWalletAssetsUseCase(session())

        balance = 1L
        useCase.invoke(Unit)

        val collector = launch { useCase.observe().collect { } }
        withTimeout(5_000) { useCase.get().first { it.data()?.policyAsset == 1L } }

        lightningLoading.value = true
        balance = 3L
        lightningLoading.value = false

        withTimeout(5_000) { useCase.get().first { it.data()?.policyAsset == 3L } }
        collector.cancel()
    }
}

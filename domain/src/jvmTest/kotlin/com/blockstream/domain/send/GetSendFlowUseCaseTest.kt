package com.blockstream.domain.send

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Assets
import com.blockstream.domain.swap.SwapUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ADDRESS = "bc1qdqnz8mzmy5wv745ntkvgjyw7mr22kr0a8a2hc5"

class GetSendFlowUseCaseTest {

    private val wallet = mockk<GreenWallet>(relaxed = true)
    private val btc = EnrichedAsset(assetId = "btc")
    private val account = AccountAsset(account = mockk(relaxed = true), asset = btc)

    private fun useCase(
        accountBalance: Long,
        addressAssets: List<EnrichedAsset> = listOf(btc),
    ): GetSendFlowUseCase {
        val session = mockk<GdkSession>(relaxed = true) {
            every { device } returns null
            every { hasLightning } returns false
            every { accountAssets(any()) } returns MutableStateFlow(Assets(mapOf(btc.assetId to accountBalance)))
        }

        return GetSendFlowUseCase(
            boltzUseCase = mockk<SwapUseCase>(relaxed = true),
            getSendAssetsUseCase = mockk { coEvery { this@mockk.invoke(any()) } returns addressAssets },
            getSendAccountsUseCase = mockk { coEvery { this@mockk.invoke(any(), any(), any(), any()) } returns emptyList() },
            prepareTransactionUseCase = mockk(relaxed = true),
            session = session,
        )
    }

    private fun errorOf(block: suspend () -> SendFlow): String? =
        runCatching { runBlocking { block() } }.exceptionOrNull()?.message

    @Test
    fun passedAccount_withBalance_goesToAmount() = runBlocking {
        val flow = useCase(accountBalance = 1_000L)(greenWallet = wallet, address = ADDRESS, asset = btc, account = account)

        assertEquals(SendFlow.SelectAmount(address = ADDRESS, account = account), flow)
    }

    @Test
    fun passedAccount_withoutBalance_reportsInsufficientFunds() = assertEquals(
        "id_insufficient_funds",
        errorOf { useCase(accountBalance = 0L)(greenWallet = wallet, address = ADDRESS, asset = btc, account = account) },
    )

    @Test
    fun noAccount_noFundedAccount_reportsInsufficientFunds() = assertEquals(
        "id_insufficient_funds",
        errorOf { useCase(accountBalance = 0L)(greenWallet = wallet, address = ADDRESS) },
    )

    @Test
    fun passedAccount_withoutBalance_addressOfAnotherAsset_reportsInvalidAddress() = assertEquals(
        "id_invalid_address",
        errorOf {
            useCase(accountBalance = 0L, addressAssets = listOf(EnrichedAsset(assetId = "other")))(
                greenWallet = wallet,
                address = ADDRESS,
                asset = btc,
                account = account,
            )
        },
    )
}

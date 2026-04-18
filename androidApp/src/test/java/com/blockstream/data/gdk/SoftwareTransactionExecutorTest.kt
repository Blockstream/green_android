package com.blockstream.data.gdk

import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Addressee
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import kotlin.test.assertEquals

class SoftwareTransactionExecutorTest {
    @Test
    fun executeSoftwareTransaction_blinds_signs_and_sends_liquid_transactions() = runTest {
        val account = account(
            network = Network(
                network = "testnet-liquid",
                name = "Liquid Testnet",
                isMainnet = false,
                isLiquid = true,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            )
        )
        val originalTransaction = transaction()
        val blindedTransaction = transaction(transaction = "blinded")
        val signedTransaction = transaction(transaction = "signed")
        val sentTransaction = ProcessedTransactionDetails(txHash = "wallet-abi-demo-tx")
        val resolver = mockk<TwoFactorResolver>(relaxed = true)
        val session = mockk<GdkSession>(relaxed = true) {
            coEvery { blindTransaction(account.network, originalTransaction) } returns blindedTransaction
            coEvery { signTransaction(account.network, blindedTransaction) } returns signedTransaction
            coEvery {
                sendTransaction(
                    account = account,
                    signedTransaction = any(),
                    isSendAll = false,
                    isBump = false,
                    twoFactorResolver = resolver
                )
            } returns sentTransaction
        }

        val result = executeSoftwareTransaction(
            session = session,
            account = account,
            transaction = originalTransaction,
            memo = "  wallet abi memo  ",
            twoFactorResolver = resolver
        )

        assertEquals(sentTransaction, result)
        coVerify(exactly = 1) { session.blindTransaction(account.network, originalTransaction) }
        coVerify(exactly = 1) { session.signTransaction(account.network, blindedTransaction) }
        coVerify(exactly = 1) {
            session.sendTransaction(
                account = account,
                signedTransaction = match { signed ->
                    signed.jsonObject["memo"]?.toString() == "\"wallet abi memo\""
                },
                isSendAll = false,
                isBump = false,
                twoFactorResolver = resolver
            )
        }
    }

    @Test
    fun executeSoftwareTransaction_skips_blinding_for_bitcoin_transactions() = runTest {
        val account = account(
            network = Network(
                network = "testnet",
                name = "Bitcoin Testnet",
                isMainnet = false,
                isLiquid = false,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            )
        )
        val originalTransaction = transaction()
        val signedTransaction = transaction(transaction = "signed")
        val sentTransaction = ProcessedTransactionDetails(txHash = "wallet-abi-demo-tx")
        val resolver = mockk<TwoFactorResolver>(relaxed = true)
        val session = mockk<GdkSession>(relaxed = true) {
            coEvery { signTransaction(account.network, originalTransaction) } returns signedTransaction
            coEvery {
                sendTransaction(
                    account = account,
                    signedTransaction = any(),
                    isSendAll = false,
                    isBump = false,
                    twoFactorResolver = resolver
                )
            } returns sentTransaction
        }

        executeSoftwareTransaction(
            session = session,
            account = account,
            transaction = originalTransaction,
            twoFactorResolver = resolver
        )

        coVerify(exactly = 0) { session.blindTransaction(any(), any()) }
        coVerify(exactly = 1) { session.signTransaction(account.network, originalTransaction) }
    }

    private fun transaction(
        transaction: String = "unsigned"
    ): CreateTransaction {
        return CreateTransaction(
            addressees = listOf(
                Addressee(
                    address = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
                    satoshi = 1_000L,
                    isGreedy = false
                )
            ),
            transaction = transaction
        ).also {
            it.jsonElement = it.toJsonElement()
        }
    }

    private fun account(network: Network): Account {
        val session = mockk<GdkSession>(relaxed = true) {
            coEvery { getCredentials(any()) } returns Credentials(mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about")
        }
        return Account(
            gdkName = "Wallet ABI",
            pointer = 0L,
            type = AccountType.BIP84_SEGWIT
        ).apply {
            setup(session = session, network = network)
        }
    }

    private companion object {
        const val TESTNET_POLICY_ASSET =
            "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    }
}

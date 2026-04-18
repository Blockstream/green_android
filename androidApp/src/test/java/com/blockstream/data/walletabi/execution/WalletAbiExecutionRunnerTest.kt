package com.blockstream.domain.walletabi.execution

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletAbiExecutionRunnerTest {
    private val account = liquidAccount()
    private val request = WalletAbiParsedRequest.TxCreate(
        WalletAbiTxCreateRequest(
            abiVersion = "wallet-abi-0.1",
            requestId = "wallet-abi-demo-request",
            network = WalletAbiNetwork.TESTNET_LIQUID,
            params = WalletAbiRuntimeParams(
                inputs = emptyList(),
                outputs = listOf(
                    WalletAbiOutput(
                        id = "output-1",
                        amountSat = 25_000L,
                        lock = buildJsonObject {
                            put("type", JsonPrimitive("script"))
                            put("script", JsonPrimitive("00140000000000000000000000000000000000000000"))
                        },
                        asset = buildJsonObject {
                            put("asset_id", JsonPrimitive(TESTNET_POLICY_ASSET))
                        },
                        blinder = buildJsonObject {
                            put("type", JsonPrimitive("rand"))
                        }
                    )
                ),
                feeRateSatKvb = 12_000f,
                lockTime = null
            ),
            broadcast = true
        )
    )
    private val plan = WalletAbiExecutionPlan(
        request = request,
        accounts = listOf(account),
        selectedAccount = account,
        destinationAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
        amountSat = 25_000L,
        assetId = TESTNET_POLICY_ASSET,
        feeRate = 12_000L
    )

    @Test
    fun execute_builds_create_transaction_params_and_returns_tx_hash() = runTest {
        val session = mockk<GdkSession>()
        val transaction = CreateTransaction(transaction = "rawtx")
        val unspentOutputs = UnspentOutputs(
            unspentOutputs = mapOf(
                TESTNET_POLICY_ASSET to listOf<JsonObject>(buildJsonObject { put("txhash", JsonPrimitive("abc")) })
            )
        )
        val captured = mutableListOf<CreateTransactionParams>()
        var executedMemo: String? = null
        var executedAccount: Account? = null
        var executedTransaction: CreateTransaction? = null
        var executedResolver: TwoFactorResolver? = null

        coEvery { session.getUnspentOutputs(account, false, false) } returns unspentOutputs
        coEvery { session.createTransaction(account.network, any()) } answers {
            captured += arg<CreateTransactionParams>(1)
            transaction
        }

        val runner = DefaultWalletAbiExecutionRunner { _, runnerAccount, runnerTransaction, memo, runnerResolver ->
            executedAccount = runnerAccount
            executedTransaction = runnerTransaction
            executedMemo = memo
            executedResolver = runnerResolver
            ProcessedTransactionDetails(txHash = "wallet-abi-tx-hash")
        }

        val result = runner.execute(
            session = session,
            plan = plan,
            twoFactorResolver = TestTwoFactorResolver
        )

        assertEquals("wallet-abi-tx-hash", result.txHash)
        assertEquals(account, executedAccount)
        assertEquals(transaction, executedTransaction)
        assertEquals("", executedMemo)
        assertEquals(TestTwoFactorResolver, executedResolver)
        assertEquals(1, captured.size)
        assertEquals(account.accountAsset, captured.single().from)
        assertEquals(12_000L, captured.single().feeRate)
        assertEquals(unspentOutputs.unspentOutputs, captured.single().utxos)
        assertEquals(plan.destinationAddress, captured.single().addresseesAsParams?.single()?.address)
        assertEquals(plan.amountSat, captured.single().addresseesAsParams?.single()?.satoshi)
        assertEquals(plan.assetId, captured.single().addresseesAsParams?.single()?.assetId)
    }

    @Test
    fun execute_requires_tx_hash_from_processed_transaction() = runTest {
        val session = mockk<GdkSession>()

        coEvery { session.getUnspentOutputs(account, false, false) } returns UnspentOutputs(emptyMap())
        coEvery { session.createTransaction(account.network, any()) } returns CreateTransaction(transaction = "rawtx")

        val runner = DefaultWalletAbiExecutionRunner { _, _, _, _, _ ->
            ProcessedTransactionDetails()
        }

        val error = assertFailsWith<IllegalStateException> {
            runner.execute(
                session = session,
                plan = plan,
                twoFactorResolver = TestTwoFactorResolver
            )
        }

        assertEquals("Wallet ABI execution did not return a transaction hash", error.message)
    }

    @Test
    fun execute_surfaces_create_transaction_error() = runTest {
        val session = mockk<GdkSession>()

        coEvery { session.getUnspentOutputs(account, false, false) } returns UnspentOutputs(emptyMap())
        coEvery { session.createTransaction(account.network, any()) } returns CreateTransaction(
            error = "id_nonconfidential_addresses_not"
        )

        val runner = DefaultWalletAbiExecutionRunner { _, _, _, _, _ ->
            error("should not execute")
        }

        val error = assertFailsWith<IllegalStateException> {
            runner.execute(
                session = session,
                plan = plan,
                twoFactorResolver = TestTwoFactorResolver
            )
        }

        assertEquals("id_nonconfidential_addresses_not", error.message)
    }

    private fun liquidAccount(): Account {
        return Account(
            networkInjected = Network(
                network = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
                name = "Liquid Testnet",
                isMainnet = false,
                isLiquid = true,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            ),
            policyAsset = EnrichedAsset(
                assetId = TESTNET_POLICY_ASSET,
                name = "Testnet Liquid Bitcoin",
                ticker = "TEST-LBTC"
            ),
            gdkName = "Liquid account",
            pointer = 0L,
            type = AccountType.BIP84_SEGWIT
        )
    }

    private data object TestTwoFactorResolver : TwoFactorResolver {
        override suspend fun selectTwoFactorMethod(availableMethods: List<String>) =
            CompletableDeferred(availableMethods.firstOrNull().orEmpty())

        override suspend fun getTwoFactorCode(
            network: Network,
            enable2faCallMethod: Boolean,
            authHandlerStatus: com.blockstream.data.gdk.data.AuthHandlerStatus
        ) = CompletableDeferred("000000")
    }

    private companion object {
        const val TESTNET_POLICY_ASSET =
            "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    }
}

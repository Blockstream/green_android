package com.blockstream.domain.walletabi.execution

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.PreparedSoftwareTransaction
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.params.AddressParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.toJsonElement
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
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
    private val preparedExecution = WalletAbiPreparedExecution(
        plan = plan,
        params = CreateTransactionParams(
            from = account.accountAsset,
            addressees = listOf(
                AddressParams(
                    address = plan.destinationAddress,
                    satoshi = plan.amountSat,
                    assetId = plan.assetId
                )
            ).toJsonElement(),
            feeRate = plan.feeRate
        ),
        transaction = CreateTransaction(transaction = "rawtx"),
        confirmation = TransactionConfirmation()
    )

    @Test
    fun prepare_uses_prepared_transaction_and_selected_account() = runTest {
        val session = mockk<GdkSession>(relaxed = true)
        val preparedTransaction = PreparedSoftwareTransaction(
            transaction = preparedExecution.transaction,
            signedTransaction = preparedExecution.transaction.toJsonElement()
        )
        var preparedAccount: Account? = null
        var preparedCreateTransaction: CreateTransaction? = null

        val runner = DefaultWalletAbiExecutionRunner(
            softwareTransactionPreparer = WalletAbiSoftwareTransactionPreparer { _, runnerAccount, runnerTransaction, memo ->
                preparedAccount = runnerAccount
                preparedCreateTransaction = runnerTransaction
                assertEquals("", memo)
                preparedTransaction
            }
        )

        val result = runner.prepare(
            session = session,
            preparedExecution = preparedExecution
        )

        assertEquals(preparedExecution, result.preparedExecution)
        assertEquals(preparedTransaction, result.preparedTransaction)
        assertEquals(account, preparedAccount)
        assertEquals(preparedExecution.transaction, preparedCreateTransaction)
    }

    @Test
    fun broadcast_returns_tx_hash_from_processed_transaction() = runTest {
        val session = mockk<GdkSession>(relaxed = true)
        val preparedBroadcast = WalletAbiPreparedBroadcast(
            preparedExecution = preparedExecution,
            preparedTransaction = PreparedSoftwareTransaction(
                transaction = preparedExecution.transaction,
                signedTransaction = preparedExecution.transaction.toJsonElement()
            )
        )
        var broadcastAccount: Account? = null
        var broadcastResolver: TwoFactorResolver? = null

        val runner = DefaultWalletAbiExecutionRunner(
            softwareTransactionBroadcaster = WalletAbiSoftwareTransactionBroadcaster { _, runnerAccount, runnerPreparedTransaction, runnerResolver ->
                broadcastAccount = runnerAccount
                broadcastResolver = runnerResolver
                assertEquals(preparedBroadcast.preparedTransaction, runnerPreparedTransaction)
                ProcessedTransactionDetails(txHash = "wallet-abi-tx-hash")
            }
        )

        val result = runner.broadcast(
            session = session,
            preparedBroadcast = preparedBroadcast,
            twoFactorResolver = TestTwoFactorResolver
        )

        assertEquals("wallet-abi-tx-hash", result.txHash)
        assertEquals(account, broadcastAccount)
        assertEquals(TestTwoFactorResolver, broadcastResolver)
    }

    @Test
    fun execute_keeps_compatibility_wrapper_behavior() = runTest {
        val session = mockk<GdkSession>(relaxed = true)
        val preparedTransaction = PreparedSoftwareTransaction(
            transaction = preparedExecution.transaction,
            signedTransaction = preparedExecution.transaction.toJsonElement()
        )
        var preparedCalled = false
        var broadcastCalled = false

        val runner = DefaultWalletAbiExecutionRunner(
            softwareTransactionPreparer = WalletAbiSoftwareTransactionPreparer { _, _, _, _ ->
                preparedCalled = true
                preparedTransaction
            },
            softwareTransactionBroadcaster = WalletAbiSoftwareTransactionBroadcaster { _, _, runnerPreparedTransaction, _ ->
                broadcastCalled = true
                assertEquals(preparedTransaction, runnerPreparedTransaction)
                ProcessedTransactionDetails(txHash = "wallet-abi-tx-hash")
            }
        )

        val result = runner.execute(
            session = session,
            preparedExecution = preparedExecution,
            twoFactorResolver = TestTwoFactorResolver
        )

        assertEquals("wallet-abi-tx-hash", result.txHash)
        assertEquals(true, preparedCalled)
        assertEquals(true, broadcastCalled)
    }

    @Test
    fun broadcast_requires_tx_hash_from_processed_transaction() = runTest {
        val session = mockk<GdkSession>(relaxed = true)
        val preparedBroadcast = WalletAbiPreparedBroadcast(
            preparedExecution = preparedExecution,
            preparedTransaction = PreparedSoftwareTransaction(
                transaction = preparedExecution.transaction,
                signedTransaction = preparedExecution.transaction.toJsonElement()
            )
        )

        val runner = DefaultWalletAbiExecutionRunner(
            softwareTransactionBroadcaster = WalletAbiSoftwareTransactionBroadcaster { _, _, _, _ ->
                ProcessedTransactionDetails()
            }
        )

        val error = assertFailsWith<IllegalStateException> {
            runner.broadcast(
                session = session,
                preparedBroadcast = preparedBroadcast,
                twoFactorResolver = TestTwoFactorResolver
            )
        }

        assertEquals("Wallet ABI execution did not return a transaction hash", error.message)
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

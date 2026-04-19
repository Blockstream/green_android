package com.blockstream.domain.walletabi.execution

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
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
    fun execute_uses_prepared_transaction_and_returns_tx_hash() = runTest {
        val session = mockk<GdkSession>(relaxed = true)
        var executedMemo: String? = null
        var executedAccount: Account? = null
        var executedTransaction: CreateTransaction? = null
        var executedResolver: TwoFactorResolver? = null

        val runner = DefaultWalletAbiExecutionRunner { _, runnerAccount, runnerTransaction, memo, runnerResolver ->
            executedAccount = runnerAccount
            executedTransaction = runnerTransaction
            executedMemo = memo
            executedResolver = runnerResolver
            ProcessedTransactionDetails(txHash = "wallet-abi-tx-hash")
        }

        val result = runner.execute(
            session = session,
            preparedExecution = preparedExecution,
            twoFactorResolver = TestTwoFactorResolver
        )

        assertEquals("wallet-abi-tx-hash", result.txHash)
        assertEquals(account, executedAccount)
        assertEquals(preparedExecution.transaction, executedTransaction)
        assertEquals("", executedMemo)
        assertEquals(TestTwoFactorResolver, executedResolver)
    }

    @Test
    fun execute_requires_tx_hash_from_processed_transaction() = runTest {
        val session = mockk<GdkSession>(relaxed = true)

        val runner = DefaultWalletAbiExecutionRunner { _, _, _, _, _ ->
            ProcessedTransactionDetails()
        }

        val error = assertFailsWith<IllegalStateException> {
            runner.execute(
                session = session,
                preparedExecution = preparedExecution,
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

package com.blockstream.domain.walletabi.execution

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.Network
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class WalletAbiExecutionPlannerTest {
    private val planner = DefaultWalletAbiExecutionPlanner(
        outputAddressResolver = WalletAbiOutputAddressResolver { output, _, _ ->
            val lock = output.lock as? kotlinx.serialization.json.JsonObject ?: return@WalletAbiOutputAddressResolver null
            if (lock["type"]?.jsonPrimitive?.content != "script") {
                return@WalletAbiOutputAddressResolver null
            }

            when (lock["script"]?.jsonPrimitive?.content) {
                VALID_SCRIPT -> "tlq1qqv6w8h0u7a2al4fj8n84g3nqu5mmz3wprw8ks4"
                else -> null
            }
        }
    )

    @Test
    fun plan_accepts_valid_single_output_policy_asset_payment() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val plan = planner.plan(
            session = session,
            request = validRequest()
        )

        assertEquals(account.id, plan.selectedAccount.id)
        assertEquals(TESTNET_POLICY_ASSET, plan.assetId)
        assertEquals(2_500L, plan.amountSat)
        assertEquals(12_000L, plan.feeRate)
        assertEquals(1, plan.accounts.size)
    }

    @Test
    fun plan_rejects_hardware_wallet_sessions() = runTest {
        val account = liquidAccount()
        val session = mockSession(
            accounts = listOf(account),
            activeAccount = account,
            isHardwareWallet = true
        )

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(session = session, request = validRequest())
        }.error

        assertEquals(WalletAbiExecutionValidationError.HardwareWalletUnsupported, error)
    }

    @Test
    fun plan_rejects_missing_eligible_liquid_accounts() = runTest {
        val account = bitcoinAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(session = session, request = validRequest())
        }.error

        assertEquals(
            WalletAbiExecutionValidationError.NoEligibleAccount(WalletAbiNetwork.TESTNET_LIQUID.wireValue),
            error
        )
    }

    @Test
    fun plan_rejects_explicit_inputs() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(
                    inputs = listOf(
                        WalletAbiInput(
                            id = "input-1",
                            utxoSource = JsonPrimitive("wallet"),
                            unblinding = JsonPrimitive("known"),
                            sequence = 1L,
                            finalizer = JsonPrimitive("default")
                        )
                    )
                )
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.ExplicitInputsUnsupported(1), error)
    }

    @Test
    fun plan_rejects_multiple_outputs() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(
                    outputs = listOf(validOutput(), validOutput(id = "output-2"))
                )
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.OutputCountUnsupported(2), error)
    }

    @Test
    fun plan_rejects_non_policy_assets() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(
                    outputs = listOf(validOutput(assetId = DIFFERENT_POLICY_ASSET))
                )
            )
        }.error

        assertEquals(
            WalletAbiExecutionValidationError.UnsupportedAsset(DIFFERENT_POLICY_ASSET),
            error
        )
    }

    @Test
    fun plan_rejects_missing_asset_id() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(
                    outputs = listOf(
                        validOutput(
                            asset = buildJsonObject {
                                put("kind", "policy")
                            }
                        )
                    )
                )
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.MissingAssetId, error)
    }

    @Test
    fun plan_rejects_unconvertible_output_scripts() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(
                    outputs = listOf(
                        validOutput(
                            lock = buildJsonObject {
                                put("type", "script")
                                put("script", "not-a-script")
                            }
                        )
                    )
                )
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.OutputLockUnsupported, error)
    }

    @Test
    fun plan_rejects_wallet_owned_outputs() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(
                    outputs = listOf(
                        validOutput(
                            blinder = buildJsonObject {
                                put("type", "wallet")
                            }
                        )
                    )
                )
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.WalletOwnedOutputUnsupported, error)
    }

    @Test
    fun plan_rejects_lock_time() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(lockTime = JsonPrimitive(144))
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.LockTimeUnsupported, error)
    }

    @Test
    fun plan_rejects_fractional_fee_rates() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(feeRateSatKvb = 12.5f)
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.InvalidFeeRate(12.5f), error)
    }

    @Test
    fun plan_rejects_non_broadcast_requests() = runTest {
        val account = liquidAccount()
        val session = mockSession(accounts = listOf(account), activeAccount = account)

        val error = assertFailsWith<WalletAbiExecutionValidationException> {
            planner.plan(
                session = session,
                request = validRequest(broadcast = false)
            )
        }.error

        assertEquals(WalletAbiExecutionValidationError.BroadcastRequired, error)
    }

    private fun validRequest(
        broadcast: Boolean = true,
        inputs: List<WalletAbiInput> = emptyList(),
        outputs: List<WalletAbiOutput> = listOf(validOutput()),
        feeRateSatKvb: Float? = 12_000f,
        lockTime: JsonPrimitive? = null
    ): WalletAbiParsedRequest {
        return WalletAbiParsedRequest.TxCreate(
            request = WalletAbiTxCreateRequest(
                abiVersion = "wallet-abi-0.1",
                requestId = "wallet-abi-demo-request",
                network = WalletAbiNetwork.TESTNET_LIQUID,
                params = WalletAbiRuntimeParams(
                    inputs = inputs,
                    outputs = outputs,
                    feeRateSatKvb = feeRateSatKvb,
                    lockTime = lockTime
                ),
                broadcast = broadcast
            )
        )
    }

    private fun validOutput(
        id: String = "output-1",
        assetId: String = TESTNET_POLICY_ASSET,
        asset: kotlinx.serialization.json.JsonObject = buildJsonObject {
            put("asset_id", assetId)
        },
        lock: kotlinx.serialization.json.JsonObject = buildJsonObject {
            put("type", "script")
            put("script", VALID_SCRIPT)
        },
        blinder: kotlinx.serialization.json.JsonObject = buildJsonObject {
            put("type", "rand")
        }
    ): WalletAbiOutput {
        return WalletAbiOutput(
            id = id,
            amountSat = 2_500L,
            lock = lock,
            asset = asset,
            blinder = blinder
        )
    }

    private fun liquidAccount(
        pointer: Long = 0L,
        name: String = "Liquid account"
    ): Account {
        return account(
            name = name,
            pointer = pointer,
            network = Network(
                network = "testnet-liquid",
                name = "Liquid Testnet",
                isMainnet = false,
                isLiquid = true,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            )
        )
    }

    private fun bitcoinAccount(): Account {
        return account(
            name = "Bitcoin account",
            pointer = 0L,
            network = Network(
                network = "testnet",
                name = "Bitcoin Testnet",
                isMainnet = false,
                isLiquid = false,
                isDevelopment = false,
                policyAsset = TESTNET_POLICY_ASSET
            )
        )
    }

    private fun account(
        name: String,
        pointer: Long,
        network: Network
    ): Account {
        val session = mockk<GdkSession>(relaxed = true)
        return Account(
            gdkName = name,
            pointer = pointer,
            type = AccountType.STANDARD
        ).apply {
            setup(session = session, network = network)
        }
    }

    private fun mockSession(
        accounts: List<Account>,
        activeAccount: Account?,
        isHardwareWallet: Boolean = false,
        mnemonic: String? = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    ): GdkSession {
        return mockk(relaxed = true) {
            every { this@mockk.accounts } returns MutableStateFlow(accounts)
            every { this@mockk.allAccounts } returns MutableStateFlow(accounts)
            every { this@mockk.activeAccount } returns MutableStateFlow(activeAccount)
            every { this@mockk.isConnected } returns true
            every { this@mockk.isHardwareWallet } returns isHardwareWallet
            coEvery { this@mockk.getCredentials(any()) } returns Credentials(mnemonic = mnemonic)
            every {
                this@mockk.updateAccountsAndBalances(any(), any(), any(), any())
            } returns Job().apply { complete() }
        }
    }

    private companion object {
        const val VALID_SCRIPT = "00146f0279e9ed041c3d710a9f57d0c02928416460c4"
        const val TESTNET_POLICY_ASSET =
            "6f0279e9ed041c3d710a9f57d0c02928416460c4b722ae3457a11eec381c526d"
        const val DIFFERENT_POLICY_ASSET =
            "5ac9f65c0efcc4775e0baec4ec03abdde22473cd3cf33c0419ca290e0751b225"
    }
}

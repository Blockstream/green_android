package com.blockstream.domain.walletabi.flow

import com.blockstream.data.walletabi.flow.WalletAbiAccountOptionPayload
import com.blockstream.data.walletabi.flow.WalletAbiApprovalTargetPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowReviewPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import com.blockstream.data.walletabi.flow.WalletAbiInputPayload
import com.blockstream.data.walletabi.flow.WalletAbiOutputPayload
import com.blockstream.data.walletabi.flow.WalletAbiParsedRequestPayload
import com.blockstream.data.walletabi.flow.WalletAbiRuntimeParamsPayload
import com.blockstream.data.walletabi.flow.WalletAbiTxCreateRequestPayload
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import org.junit.Test

class WalletAbiFlowSnapshotRepositoryTest {
    private val store = mockk<WalletAbiFlowSnapshotStore>()
    private val repository = WalletAbiFlowSnapshotRepository(store)

    private val parsedRequest = WalletAbiParsedRequest.TxCreate(
        request = WalletAbiTxCreateRequest(
            abiVersion = "wallet-abi-0.1",
            requestId = "request-id",
            network = WalletAbiNetwork.TESTNET_LIQUID,
            params = WalletAbiRuntimeParams(
                inputs = listOf(
                    WalletAbiInput(
                        id = "input-1",
                        utxoSource = JsonPrimitive("utxo-source"),
                        unblinding = JsonPrimitive("unblinding"),
                        sequence = 1L,
                        finalizer = JsonPrimitive("finalizer")
                    )
                ),
                outputs = listOf(
                    WalletAbiOutput(
                        id = "output-1",
                        amountSat = 1_000L,
                        lock = JsonPrimitive("lock"),
                        asset = JsonPrimitive("asset"),
                        blinder = JsonPrimitive("blinder")
                    )
                ),
                feeRateSatKvb = 12.5f,
                lockTime = JsonPrimitive("lock-time")
            ),
            broadcast = true
        )
    )

    private val snapshot = WalletAbiResumeSnapshot(
        review = WalletAbiFlowReview(
            requestContext = WalletAbiStartRequestContext(
                requestId = "request-id",
                walletId = "wallet-id"
            ),
            title = "Demo payment",
            message = "Approve a parsed Wallet ABI request",
            accounts = listOf(
                WalletAbiAccountOption(
                    accountId = "account-1",
                    name = "Main account"
                )
            ),
            selectedAccountId = "account-1",
            approvalTarget = WalletAbiApprovalTarget.Software,
            parsedRequest = parsedRequest
        ),
        phase = WalletAbiResumePhase.REQUEST_LOADED
    )

    private val payload = WalletAbiFlowSnapshotPayload(
        review = WalletAbiFlowReviewPayload(
            requestId = "request-id",
            walletId = "wallet-id",
            title = "Demo payment",
            message = "Approve a parsed Wallet ABI request",
            accounts = listOf(
                WalletAbiAccountOptionPayload(
                    accountId = "account-1",
                    name = "Main account"
                )
            ),
            selectedAccountId = "account-1",
            approvalTarget = WalletAbiApprovalTargetPayload(
                kind = "software"
            ),
            parsedRequest = WalletAbiParsedRequestPayload(
                kind = "tx_create",
                txCreate = WalletAbiTxCreateRequestPayload(
                    abiVersion = "wallet-abi-0.1",
                    requestId = "request-id",
                    network = "testnet-liquid",
                    params = WalletAbiRuntimeParamsPayload(
                        inputs = listOf(
                            WalletAbiInputPayload(
                                id = "input-1",
                                utxoSource = JsonPrimitive("utxo-source"),
                                unblinding = JsonPrimitive("unblinding"),
                                sequence = 1L,
                                finalizer = JsonPrimitive("finalizer")
                            )
                        ),
                        outputs = listOf(
                            WalletAbiOutputPayload(
                                id = "output-1",
                                amountSat = 1_000L,
                                lock = JsonPrimitive("lock"),
                                asset = JsonPrimitive("asset"),
                                blinder = JsonPrimitive("blinder")
                            )
                        ),
                        feeRateSatKvb = 12.5f,
                        lockTime = JsonPrimitive("lock-time")
                    ),
                    broadcast = true
                )
            )
        ),
        phase = WalletAbiResumePhase.REQUEST_LOADED.name
    )

    @Test
    fun save_includes_parsed_request() = runTest {
        val payloadSlot = slot<WalletAbiFlowSnapshotPayload>()
        coEvery { store.save("wallet-id", capture(payloadSlot)) } returns Unit

        repository.save("wallet-id", snapshot)

        assertEquals(payload, payloadSlot.captured)
    }

    @Test
    fun load_restores_parsed_request() = runTest {
        coEvery { store.load("wallet-id") } returns payload

        assertEquals(snapshot, repository.load("wallet-id"))
        coVerify(exactly = 1) { store.load("wallet-id") }
    }
}

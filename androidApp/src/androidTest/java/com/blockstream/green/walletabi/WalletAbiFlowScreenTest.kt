package com.blockstream.green.walletabi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.models.walletabi.WalletAbiReviewLook
import com.blockstream.compose.screens.walletabi.WalletAbiFlowScreen
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionDetails
import com.blockstream.domain.walletabi.flow.WalletAbiFlowError
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiJadeContext
import com.blockstream.domain.walletabi.flow.WalletAbiJadeEvent
import com.blockstream.domain.walletabi.flow.WalletAbiJadeStep
import com.blockstream.domain.walletabi.flow.WalletAbiResumePhase
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class WalletAbiFlowScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<WalletAbiComposeTestActivity>()

    private val requestContext = WalletAbiStartRequestContext(
        requestId = "request-id",
        walletId = "wallet-id"
    )

    private val softwareReview = WalletAbiFlowReview(
        requestContext = requestContext,
        title = "Send",
        message = "Review this Wallet ABI request",
        accounts = listOf(
            com.blockstream.domain.walletabi.flow.WalletAbiAccountOption("account-1", "Main"),
            com.blockstream.domain.walletabi.flow.WalletAbiAccountOption("account-2", "Savings")
        ),
        selectedAccountId = "account-1",
        approvalTarget = WalletAbiApprovalTarget.Software,
        executionDetails = WalletAbiExecutionDetails(
            destinationAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
            amountSat = 1_000L,
            assetId = "asset-id",
            network = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
            feeRate = 12_000L
        )
    )

    private val jadeReview = softwareReview.copy(
        approvalTarget = WalletAbiApprovalTarget.Jade(
            deviceName = "Jade",
            deviceId = "jade-id"
        )
    )
    private val softwareReviewLook = WalletAbiReviewLook(
        accountAssetBalance = AccountAssetBalance(
            account = previewAccountAsset().account,
            asset = previewAccountAsset().asset
        ),
        recipientAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
        amount = "1,000 TEST-LBTC",
        amountFiat = "0.10 USD",
        assetName = "Testnet Liquid Bitcoin",
        assetTicker = "TEST-LBTC",
        assetId = "asset-id",
        networkName = "Liquid Testnet",
        networkWireValue = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
        method = "wallet_abi_process_request",
        abiVersion = "wallet-abi-0.1",
        requestId = "parsed-request-id",
        broadcast = true,
        recipientScript = "00140000000000000000000000000000000000000000",
        transactionConfirmation = TransactionConfirmation(
            utxos = listOf(
                UtxoView(
                    address = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
                    assetId = "asset-id",
                    satoshi = 1_000L,
                    amount = "1,000 TEST-LBTC",
                    amountExchange = "0.10 USD"
                )
            ),
            fee = "0.01 TEST-LBTC",
            feeFiat = "0.00 USD",
            feeRate = "12 sat/vB",
            total = "1,000.01 TEST-LBTC",
            totalFiat = "0.10 USD"
        )
    )

    @Test
    fun walletAbiFlowScreen_mountsOnEmulator() {
        setScreen(WalletAbiFlowState.Loading(requestContext))

        composeRule.onNodeWithTag("wallet_abi_flow_title").assertIsDisplayed()
    }

    @Test
    fun walletAbiFlowScreen_shows_loading_state() {
        setScreen(WalletAbiFlowState.Loading(requestContext))

        composeRule.onNodeWithTag("wallet_abi_flow_loading").assertIsDisplayed()
    }

    @Test
    fun walletAbiFlowScreen_dispatches_software_approval() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(WalletAbiFlowState.RequestLoaded(softwareReview), intents::add)

        composeRule.onNodeWithTag("wallet_abi_flow_account_1").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_reject_action").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()

        assertEquals(
            listOf(
                WalletAbiFlowIntent.SelectAccount("account-2"),
                WalletAbiFlowIntent.Reject,
                WalletAbiFlowIntent.Approve
            ),
            intents
        )
    }

    @Test
    fun walletAbiFlowScreen_shows_parsed_request_facts() {
        setScreen(
            WalletAbiFlowState.RequestLoaded(
                softwareReview.copy(
                    parsedRequest = WalletAbiParsedRequest.TxCreate(
                        request = WalletAbiTxCreateRequest(
                            abiVersion = "wallet-abi-0.1",
                            requestId = "parsed-request-id",
                            network = WalletAbiNetwork.TESTNET_LIQUID,
                            params = WalletAbiRuntimeParams(
                                inputs = listOf(
                                    WalletAbiInput(
                                        id = "input-1",
                                        utxoSource = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("wallet")) },
                                        unblinding = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("known")) },
                                        sequence = 1L,
                                        finalizer = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("default")) }
                                    )
                                ),
                                outputs = listOf(
                                    WalletAbiOutput(
                                        id = "output-1",
                                        amountSat = 1000L,
                                        lock = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("pkh")) },
                                        asset = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("btc")) },
                                        blinder = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("default")) }
                                    ),
                                    WalletAbiOutput(
                                        id = "output-2",
                                        amountSat = 2000L,
                                        lock = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("pkh")) },
                                        asset = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("btc")) },
                                        blinder = kotlinx.serialization.json.buildJsonObject { put("kind", JsonPrimitive("default")) }
                                    )
                                ),
                                feeRateSatKvb = 12_000f
                            ),
                            broadcast = true
                        )
                    ),
                    executionDetails = WalletAbiExecutionDetails(
                        destinationAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
                        amountSat = 1_000L,
                        assetId = "asset-id",
                        network = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
                        feeRate = 12_000L
                    )
                )
            ),
            reviewLook = softwareReviewLook
        )

        composeRule.onNodeWithTag("wallet_abi_flow_review_warning").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_selected_account").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_destination").assertIsDisplayed()
    }

    @Test
    fun walletAbiFlowScreen_shows_jade_request_variant() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(WalletAbiFlowState.RequestLoaded(jadeReview), intents::add)

        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()

        assertEquals(
            listOf<WalletAbiFlowIntent>(WalletAbiFlowIntent.Approve),
            intents
        )
    }

    @Test
    fun walletAbiFlowScreen_dispatches_jade_unlock_cancel() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(
            WalletAbiFlowState.AwaitingApproval(
                requestContext = requestContext,
                selectedAccountId = "account-1",
                jade = WalletAbiJadeContext(
                    deviceId = "jade-id",
                    step = WalletAbiJadeStep.UNLOCK,
                    message = null,
                    retryable = false
                )
            ),
            intents::add
        )

        composeRule.onNodeWithTag("wallet_abi_flow_awaiting_approval").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_jade_cancel_action").performClick()

        assertEquals(
            listOf<WalletAbiFlowIntent>(
                WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Cancelled)
            ),
            intents
        )
    }

    @Test
    fun walletAbiFlowScreen_shows_jade_sign_step() {
        setScreen(
            WalletAbiFlowState.AwaitingApproval(
                requestContext = requestContext,
                selectedAccountId = "account-1",
                jade = WalletAbiJadeContext(
                    deviceId = "jade-id",
                    step = WalletAbiJadeStep.SIGN,
                    message = null,
                    retryable = false
                )
            )
        )

        composeRule.onNodeWithTag("wallet_abi_flow_awaiting_approval").assertIsDisplayed()
    }

    @Test
    fun walletAbiFlowScreen_shows_submitting_state() {
        setScreen(
            WalletAbiFlowState.Submitting(
                requestContext = requestContext,
                jade = WalletAbiJadeContext(
                    deviceId = "jade-id",
                    step = WalletAbiJadeStep.SIGN,
                    message = null,
                    retryable = false
                )
            )
        )

        composeRule.onNodeWithTag("wallet_abi_flow_submitting").assertIsDisplayed()
    }

    @Test
    fun walletAbiFlowScreen_dispatches_resumable_actions() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(
            WalletAbiFlowState.Resumable(
                WalletAbiResumeSnapshot(
                    review = softwareReview,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            ),
            intents::add
        )

        composeRule.onNodeWithTag("wallet_abi_flow_resume_action").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_cancel_resume_action").performClick()

        assertEquals(
            listOf(
                WalletAbiFlowIntent.Resume,
                WalletAbiFlowIntent.CancelResume
            ),
            intents
        )
    }

    @Test
    fun walletAbiFlowScreen_dispatches_success_dismiss() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(
            WalletAbiFlowState.Success(
                WalletAbiSuccessResult(
                    requestId = "request-id",
                    txHash = "tx-hash",
                    responseId = "response-id"
                )
            ),
            intents::add
        )

        composeRule.onNodeWithTag("wallet_abi_flow_success").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_success_tx_hash").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_terminal_dismiss_action").performClick()

        assertEquals(
            listOf<WalletAbiFlowIntent>(WalletAbiFlowIntent.DismissTerminal),
            intents
        )
    }

    @Test
    fun walletAbiFlowScreen_dispatches_cancelled_dismiss() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(
            WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected),
            intents::add
        )

        composeRule.onNodeWithTag("wallet_abi_flow_cancelled").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_terminal_dismiss_action").performClick()

        assertEquals(
            listOf<WalletAbiFlowIntent>(WalletAbiFlowIntent.DismissTerminal),
            intents
        )
    }

    @Test
    fun walletAbiFlowScreen_dispatches_error_actions() {
        val intents = mutableListOf<WalletAbiFlowIntent>()
        setScreen(
            WalletAbiFlowState.Error(WalletAbiFlowError("Execution failed")),
            intents::add
        )

        composeRule.onNodeWithTag("wallet_abi_flow_error").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_retry_action").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_terminal_dismiss_action").performClick()

        assertEquals(
            listOf(
                WalletAbiFlowIntent.Retry,
                WalletAbiFlowIntent.DismissTerminal
            ),
            intents
        )
    }

    private fun setScreen(
        state: WalletAbiFlowState,
        onIntent: (WalletAbiFlowIntent) -> Unit = {},
        reviewLook: WalletAbiReviewLook? = null
    ) {
        composeRule.setContent {
            GreenPreview {
                WalletAbiFlowScreen(
                    state = state,
                    onIntent = onIntent,
                    reviewLook = reviewLook
                )
            }
        }
    }
}

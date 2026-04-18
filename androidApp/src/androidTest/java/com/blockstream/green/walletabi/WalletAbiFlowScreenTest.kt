package com.blockstream.green.walletabi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.screens.walletabi.WalletAbiFlowScreen
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
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
        approvalTarget = WalletAbiApprovalTarget.Software
    )

    private val jadeReview = softwareReview.copy(
        approvalTarget = WalletAbiApprovalTarget.Jade(
            deviceName = "Jade",
            deviceId = "jade-id"
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
        composeRule.onNodeWithTag("wallet_abi_flow_resolve_action").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_reject_action").performClick()
        composeRule.onNodeWithTag("wallet_abi_flow_approve_action").performClick()

        assertEquals(
            listOf(
                WalletAbiFlowIntent.SelectAccount("account-2"),
                WalletAbiFlowIntent.ResolveRequest,
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
                                feeRateSatKvb = 12.5f
                            ),
                            broadcast = true
                        )
                    )
                )
            )
        )

        composeRule.onNodeWithTag("wallet_abi_flow_parsed_request_id").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_parsed_network").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_parsed_broadcast").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_parsed_input_count").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_abi_flow_parsed_output_count").assertIsDisplayed()
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
                    responseId = "response-id"
                )
            ),
            intents::add
        )

        composeRule.onNodeWithTag("wallet_abi_flow_success").assertIsDisplayed()
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
        onIntent: (WalletAbiFlowIntent) -> Unit = {}
    ) {
        composeRule.setContent {
            GreenPreview {
                WalletAbiFlowScreen(
                    state = state,
                    onIntent = onIntent
                )
            }
        }
    }
}

package com.blockstream.compose.screens.walletabi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_from
import blockstream_green.common.generated.resources.id_network
import blockstream_green.common.generated.resources.id_recipient_address
import blockstream_green.common.generated.resources.id_review
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.TransactionConfirmationSummary
import com.blockstream.compose.extensions.previewAccountAssetBalance
import com.blockstream.compose.models.walletabi.WalletAbiFlowRouteViewModel
import com.blockstream.compose.models.walletabi.WalletAbiFlowViewModel
import com.blockstream.compose.models.walletabi.WalletAbiReviewAssetImpactLook
import com.blockstream.compose.models.walletabi.WalletAbiReviewLook
import com.blockstream.compose.models.walletabi.WalletAbiReviewOutputLook
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionDetails
import com.blockstream.domain.walletabi.flow.WalletAbiFlowError
import com.blockstream.domain.walletabi.flow.WalletAbiFlowErrorKind
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowPhase
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiJadeContext
import com.blockstream.domain.walletabi.flow.WalletAbiJadeStep
import com.blockstream.domain.walletabi.flow.WalletAbiRequestFamily
import com.blockstream.domain.walletabi.flow.WalletAbiResolutionState
import com.blockstream.domain.walletabi.flow.WalletAbiResumePhase
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSubmittingStage
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val TESTNET_POLICY_ASSET = "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"

@Composable
fun WalletAbiFlowScreen(viewModel: WalletAbiFlowViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WalletAbiFlowScreen(
        state = state,
        onIntent = viewModel::dispatch
    )
}

@Composable
fun WalletAbiFlowScreen(viewModel: WalletAbiFlowRouteViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reviewLook by viewModel.reviewLook.collectAsStateWithLifecycle()
    WalletAbiFlowScreen(
        state = state,
        onIntent = viewModel::dispatch,
        reviewLook = reviewLook
    )
}

@Composable
fun WalletAbiFlowScreen(
    state: WalletAbiFlowState,
    onIntent: (WalletAbiFlowIntent) -> Unit,
    reviewLook: WalletAbiReviewLook? = null
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = state, label = "wallet_abi_state") { currentState ->
            when (currentState) {
                WalletAbiFlowState.Idle -> {
                    WalletAbiScreenScaffold(body = {
                        WalletAbiStateHero(
                            title = "No active Wallet ABI request",
                            supporting = "Open a Wallet ABI request from the wallet flow before this screen can show a review or approval step."
                        )
                        GreenAlert(
                            title = "Wallet ABI is idle",
                            message = "This screen only becomes active when a Wallet ABI request is loaded for review.",
                            isBlue = true
                        )
                    })
                }

                is WalletAbiFlowState.Loading -> {
                    LoadingContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.RequestLoaded -> {
                    RequestLoadedContent(
                        state = currentState,
                        reviewLook = reviewLook,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.AwaitingApproval -> {
                    AwaitingApprovalContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.Submitting -> {
                    SubmittingContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.Success -> {
                    SuccessContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.Cancelled -> {
                    CancelledContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.Error -> {
                    ErrorContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.Resumable -> {
                    ResumableContent(
                        state = currentState,
                        onIntent = onIntent
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletAbiScreenScaffold(
    body: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WalletAbiScreenHeader()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = body
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = footer
        )
    }
}

@Composable
private fun WalletAbiScreenHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Wallet ABI request",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("wallet_abi_flow_title")
        )
        Text(
            text = "Wallet ABI can request real wallet actions. Review each step carefully before approving.",
            style = bodyMedium,
            color = whiteMedium
        )
    }
}

@Composable
private fun WalletAbiStateHero(
    title: String,
    supporting: String,
    testTag: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
        Text(
            text = supporting,
            style = bodyMedium,
            color = whiteMedium
        )
    }
}

@Composable
private fun LoadingContent(
    state: WalletAbiFlowState.Loading,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            )
            WalletAbiStateHero(
                title = if (state.isCancelling) {
                    "Cancelling Wallet ABI request"
                } else {
                    "Loading Wallet ABI request"
                },
                supporting = if (state.isCancelling) {
                    "The app is stopping the pending request before any transaction is broadcast."
                } else {
                    "The app is validating the request and rebuilding the exact review before you can approve anything."
                },
                testTag = "wallet_abi_flow_loading"
            )
            GreenAlert(
                title = if (state.isCancelling) {
                    "Stopping before broadcast"
                } else {
                    "No signature or broadcast yet"
                },
                message = if (state.isCancelling) {
                    "Cancellation is in progress. Wallet state should remain unchanged."
                } else {
                    "Wallet ABI only moves to approval after the exact request review is ready."
                },
                isBlue = true
            )
            GreenDataLayout(title = "Request", testTag = "wallet_abi_flow_request_context") {
                DetailRow(
                    label = "Request ID",
                    value = state.requestContext.requestId,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        footer = {
            GreenButton(
                text = "Cancel request",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_cancel_action",
                enabled = !state.isCancelling,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                onIntent(WalletAbiFlowIntent.Cancel)
            }
        }
    )
}

@Composable
private fun AwaitingApprovalContent(
    state: WalletAbiFlowState.AwaitingApproval,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = if (state.isCancelling) {
                    "Cancelling Wallet ABI approval"
                } else {
                    state.jade.awaitingTitle()
                },
                supporting = if (state.isCancelling) {
                    "The app is stopping the approval flow before any transaction is broadcast."
                } else {
                    state.jade.awaitingBody()
                },
                testTag = "wallet_abi_flow_awaiting_approval"
            )
            GreenAlert(
                title = "Approval continues on Jade",
                message = "Confirm only if the device details match the Wallet ABI request you just reviewed.",
                isBlue = true
            )
            GreenDataLayout(title = "Device action", testTag = "wallet_abi_flow_jade_details") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "Current step", value = state.jade.step.label())
                    state.jade.message?.takeIf { it.isNotBlank() }?.let { message ->
                        DetailRow(label = "Device message", value = message)
                    }
                }
            }
        },
        footer = {
            GreenButton(
                text = "Cancel request",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_jade_cancel_action",
                enabled = !state.isCancelling,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                onIntent(WalletAbiFlowIntent.Cancel)
            }
        }
    )
}

@Composable
private fun SubmittingContent(
    state: WalletAbiFlowState.Submitting,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            )
            WalletAbiStateHero(
                title = state.submittingTitle(),
                supporting = state.submittingBody(),
                testTag = "wallet_abi_flow_submitting"
            )
            GreenAlert(
                title = if (state.stage == WalletAbiSubmittingStage.BROADCASTING) {
                    "Broadcast in progress"
                } else {
                    "Not yet broadcast"
                },
                message = if (state.stage == WalletAbiSubmittingStage.BROADCASTING) {
                    "The app already started handing the reviewed transaction to the Liquid network. Wallet or network state may already change."
                } else {
                    "The reviewed transaction is still being prepared. You can cancel until broadcasting starts."
                },
                isBlue = state.stage == WalletAbiSubmittingStage.PREPARING
            )
            GreenDataLayout(title = "Submission stage", testTag = "wallet_abi_flow_submission_stage") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "Current stage", value = state.stage.label())
                    state.jade?.let { jade ->
                        DetailRow(label = "Device step", value = jade.step.label())
                    }
                }
            }
        },
        footer = {
            if (state.stage == WalletAbiSubmittingStage.PREPARING) {
                GreenButton(
                    text = "Cancel request",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "wallet_abi_flow_cancel_action",
                    enabled = !state.isCancelling,
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE
                ) {
                    onIntent(WalletAbiFlowIntent.Cancel)
                }
            }
        }
    )
}

@Composable
private fun SuccessContent(
    state: WalletAbiFlowState.Success,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = "Wallet ABI request completed",
                supporting = "The reviewed transaction was signed and broadcast from the selected wallet.",
                testTag = "wallet_abi_flow_success"
            )
            GreenAlert(
                title = "Transaction broadcast",
                message = "Keep the transaction hash if you want to track confirmation on the network.",
                isBlue = true
            )
            GreenDataLayout(title = "Transaction hash", testTag = "wallet_abi_flow_success_hash_panel") {
                ReviewValueColumn(
                    primary = state.result.txHash ?: state.result.responseId ?: "unavailable",
                    secondary = state.result.responseId
                        ?.takeIf { state.result.txHash != null }
                        ?.let { "Response ID: $it" },
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("wallet_abi_flow_success_tx_hash")
                )
            }
            GreenDataLayout(title = "Request", testTag = "wallet_abi_flow_success_request") {
                DetailRow(
                    label = "Request ID",
                    value = state.result.requestId,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        footer = {
            GreenButton(
                text = "Done",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_terminal_dismiss_action"
            ) {
                onIntent(WalletAbiFlowIntent.DismissTerminal)
            }
        }
    )
}

@Composable
private fun CancelledContent(
    state: WalletAbiFlowState.Cancelled,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = state.reason.cancelledTitle(),
                supporting = state.reason.cancelledBody(),
                testTag = "wallet_abi_flow_cancelled"
            )
            GreenDataLayout(title = "Status", testTag = "wallet_abi_flow_cancelled_status") {
                DetailRow(
                    label = "Reason",
                    value = state.reason.label(),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        footer = {
            GreenButton(
                text = "Done",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_terminal_dismiss_action"
            ) {
                onIntent(WalletAbiFlowIntent.DismissTerminal)
            }
        }
    )
}

@Composable
private fun ErrorContent(
    state: WalletAbiFlowState.Error,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = state.error.title(),
                supporting = state.error.body(),
                testTag = "wallet_abi_flow_error_title"
            )
            Text(
                text = state.error.body(),
                style = bodyMedium,
                color = whiteMedium,
                modifier = Modifier.testTag("wallet_abi_flow_error")
            )
            GreenAlert(
                title = when {
                    state.error.kind == WalletAbiFlowErrorKind.PARTIAL_COMPLETION ->
                        "Do not retry automatically"
                    state.error.retryable ->
                        "Retry is available"
                    else ->
                        "Request blocked"
                },
                message = when {
                    state.error.kind == WalletAbiFlowErrorKind.PARTIAL_COMPLETION ->
                        "The transaction may already have reached the network. Review wallet state before taking any further action."
                    state.error.retryable ->
                        "Retry rebuilds the Wallet ABI request and exact review before another approval attempt."
                    else ->
                        "Fix the Wallet ABI request or wallet setup before trying again."
                },
                isBlue = state.error.retryable
            )
            GreenDataLayout(title = "Error details", testTag = "wallet_abi_flow_error_details") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "Phase", value = state.error.phase.label())
                    DetailRow(label = "Retryable", value = state.error.retryable.toString())
                }
            }
        },
        footer = {
            if (state.error.retryable) {
                GreenButton(
                    text = "Retry",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "wallet_abi_flow_retry_action"
                ) {
                    onIntent(WalletAbiFlowIntent.Retry)
                }
            }
            GreenButton(
                text = "Dismiss",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_terminal_dismiss_action",
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                onIntent(WalletAbiFlowIntent.DismissTerminal)
            }
        }
    )
}

@Composable
private fun ResumableContent(
    state: WalletAbiFlowState.Resumable,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = state.snapshot.phase.resumeTitle(),
                supporting = state.snapshot.phase.resumeBody(),
                testTag = "wallet_abi_flow_resumable"
            )
            GreenAlert(
                title = "Pending Wallet ABI request",
                message = "Resume rebuilds the current review from wallet data before continuing. Cancel clears the pending request.",
                isBlue = true
            )
            GreenDataLayout(title = "Pending request", testTag = "wallet_abi_flow_resume_details") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(
                        label = "Request ID",
                        value = state.snapshot.review.requestContext.requestId
                    )
                    DetailRow(
                        label = "Saved phase",
                        value = state.snapshot.phase.label(),
                        testTag = "wallet_abi_flow_resume_phase"
                    )
                    state.snapshot.review.selectedAccountId?.let { selectedAccountId ->
                        DetailRow(label = "Selected account", value = selectedAccountId)
                    }
                }
            }
        },
        footer = {
            GreenButton(
                text = "Resume flow",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_resume_action"
            ) {
                onIntent(WalletAbiFlowIntent.Resume)
            }
            GreenButton(
                text = "Cancel resume",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_cancel_resume_action",
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                onIntent(WalletAbiFlowIntent.CancelResume)
            }
        }
    )
}

@Composable
private fun RequestLoadedContent(
    state: WalletAbiFlowState.RequestLoaded,
    reviewLook: WalletAbiReviewLook?,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    if (reviewLook == null) {
        LegacyRequestLoadedContent(state = state, onIntent = onIntent)
        return
    }

    var technicalDetailsExpanded by rememberSaveable(state.review.requestContext.requestId) {
        mutableStateOf(false)
    }
    val approvalText = if (state.review.approvalTarget is WalletAbiApprovalTarget.Jade) {
        "Approve with Jade"
    } else {
        "Approve and broadcast"
    }

    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = "Review Wallet ABI request",
                supporting = reviewLook.reviewIntro(),
                testTag = "wallet_abi_flow_request_title"
            )

            reviewLook.statusMessage?.takeIf { it.isNotBlank() }?.let {
                Crossfade(
                    targetState = reviewLook.resolutionState,
                    label = "wallet_abi_resolution_state"
                ) { resolutionState ->
                    GreenAlert(
                        title = resolutionState.alertTitle(),
                        message = reviewLook.statusMessage,
                        modifier = Modifier.testTag("wallet_abi_flow_resolution_status"),
                        isBlue = resolutionState == WalletAbiResolutionState.READY
                    )
                }
            }

            GreenAlert(
                title = reviewLook.reviewWarningTitle(),
                message = reviewLook.reviewWarningBody(),
                modifier = Modifier.testTag("wallet_abi_flow_review_warning")
            )

            GreenAccountAsset(
                accountAssetBalance = reviewLook.accountAssetBalance,
                title = stringResource(Res.string.id_from),
                withAsset = false,
                modifier = Modifier.testTag("wallet_abi_flow_selected_account")
            )

            if (state.review.accounts.size > 1) {
                GreenDataLayout(
                    title = "Funding account",
                    testTag = "wallet_abi_flow_account_switcher"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Choose which eligible account should fund this Wallet ABI request.",
                            style = bodySmall,
                            color = whiteMedium
                        )
                        state.review.accounts.forEachIndexed { index, account ->
                            GreenButton(
                                text = if (account.accountId == state.review.selectedAccountId) {
                                    "${account.name} (selected)"
                                } else {
                                    account.name
                                },
                                enabled = !reviewLook.isRefreshing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_abi_flow_account_$index"),
                                type = GreenButtonType.OUTLINE,
                                color = GreenButtonColor.WHITE
                            ) {
                                onIntent(WalletAbiFlowIntent.SelectAccount(account.accountId))
                            }
                        }
                    }
                }
            }

            GreenDataLayout(
                title = if (reviewLook.outputs.size == 1) {
                    stringResource(Res.string.id_recipient_address)
                } else {
                    "Requested outputs"
                },
                testTag = "wallet_abi_flow_outputs_summary"
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    reviewLook.outputs.forEachIndexed { index, output ->
                        RequestedOutputRow(
                            index = index,
                            output = output
                        )
                    }
                }
            }

            GreenDataLayout(
                title = stringResource(Res.string.id_amount),
                testTag = "wallet_abi_flow_amount_summary"
            ) {
                ReviewValueColumn(
                    primary = reviewLook.amountSummaryPrimary(),
                    secondary = reviewLook.amountSummarySecondary(),
                    modifier = Modifier.padding(16.dp)
                )
            }

            GreenDataLayout(
                title = "Asset",
                testTag = "wallet_abi_flow_asset_summary"
            ) {
                ReviewValueColumn(
                    primary = reviewLook.assetSummaryPrimary(),
                    secondary = reviewLook.assetId,
                    modifier = Modifier.padding(16.dp)
                )
            }

            GreenDataLayout(
                title = stringResource(Res.string.id_network),
                testTag = "wallet_abi_flow_network_summary"
            ) {
                ReviewValueColumn(
                    primary = reviewLook.networkName,
                    secondary = reviewLook.networkWireValue,
                    modifier = Modifier.padding(16.dp)
                )
            }

            GreenDataLayout(
                title = "Fees and total",
                testTag = "wallet_abi_flow_fees_summary"
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reviewLook.transactionConfirmation?.let { confirmation ->
                        TransactionConfirmationSummary(confirmation = confirmation)
                    } ?: Text(
                        text = "Exact fees and total become available after the Wallet ABI request resolves to a final review.",
                        style = bodyMedium,
                        color = whiteMedium
                    )
                }
            }

            if (reviewLook.assetImpacts.isNotEmpty()) {
                GreenDataLayout(
                    title = "Asset impacts",
                    testTag = "wallet_abi_flow_asset_impacts"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reviewLook.assetImpacts.forEachIndexed { index, impact ->
                            AssetImpactRow(
                                impact = impact,
                                modifier = Modifier.testTag("wallet_abi_flow_asset_impact_$index")
                            )
                        }
                    }
                }
            }

            if (reviewLook.warnings.isNotEmpty()) {
                GreenDataLayout(
                    title = "Warnings",
                    testTag = "wallet_abi_flow_warnings"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reviewLook.warnings.forEachIndexed { index, warning ->
                            Text(
                                text = warning,
                                style = bodySmall,
                                modifier = Modifier.testTag("wallet_abi_flow_warning_$index")
                            )
                        }
                    }
                }
            }

            if (state.review.approvalTarget is WalletAbiApprovalTarget.Jade) {
                GreenAlert(
                    title = "Approval continues on Jade",
                    message = "After this review, confirm the same details on Jade before the Wallet ABI request can continue.",
                    isBlue = true
                )
            }

            AnimatedVisibility(visible = reviewLook.isRefreshing) {
                GreenAlert(
                    title = "Refreshing exact review",
                    message = "The app is rebuilding the exact transaction preview for the selected account.",
                    modifier = Modifier.testTag("wallet_abi_flow_review_refreshing"),
                    isBlue = true
                )
            }

            GreenButton(
                text = if (technicalDetailsExpanded) {
                    "Hide technical request details"
                } else {
                    "Show technical request details"
                },
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE,
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_technical_details_toggle"
            ) {
                technicalDetailsExpanded = !technicalDetailsExpanded
            }

            AnimatedVisibility(visible = technicalDetailsExpanded) {
                GreenDataLayout(
                    title = "Technical details",
                    testTag = "wallet_abi_flow_technical_details"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow("Method", reviewLook.method)
                        DetailRow("ABI version", reviewLook.abiVersion)
                        DetailRow(
                            label = "Request ID",
                            value = reviewLook.requestId,
                            testTag = "wallet_abi_flow_parsed_request_id"
                        )
                        DetailRow("Broadcast", reviewLook.broadcast.toString())
                        DetailRow(
                            label = "Network",
                            value = reviewLook.networkWireValue,
                            testTag = "wallet_abi_flow_parsed_network"
                        )
                        DetailRow("Request family", reviewLook.requestFamily.label())
                        DetailRow("Requested outputs", reviewLook.outputs.size.toString())
                        DetailRow("Selected account", state.review.selectedAccountId ?: "n/a")
                        DetailRow(
                            label = "Asset ID",
                            value = reviewLook.assetId,
                            testTag = "wallet_abi_flow_asset"
                        )
                        reviewLook.transactionConfirmation?.feeRate?.also { feeRate ->
                            DetailRow(
                                label = "Fee rate",
                                value = feeRate,
                                testTag = "wallet_abi_flow_fee_rate"
                            )
                        }
                        reviewLook.outputs.forEachIndexed { index, output ->
                            output.recipientScript?.also { script ->
                                DetailRow(
                                    label = "Output ${index + 1} script",
                                    value = script
                                )
                            }
                        }
                    }
                }
            }
        },
        footer = {
            GreenButton(
                text = "Reject request",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_reject_action",
                enabled = !reviewLook.isRefreshing,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                onIntent(WalletAbiFlowIntent.Reject)
            }
            if (reviewLook.canResolve) {
                GreenButton(
                    text = "Resolve exact review",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "wallet_abi_flow_resolve_action",
                    enabled = !reviewLook.isRefreshing,
                    onProgress = reviewLook.isRefreshing,
                    size = GreenButtonSize.LARGE
                ) {
                    onIntent(WalletAbiFlowIntent.ResolveRequest)
                }
            }
            GreenButton(
                text = approvalText,
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_approve_action",
                enabled = reviewLook.canApprove && !reviewLook.isRefreshing,
                onProgress = reviewLook.isRefreshing,
                size = GreenButtonSize.LARGE
            ) {
                onIntent(WalletAbiFlowIntent.Approve)
            }
        }
    )
}

@Composable
private fun RequestedOutputRow(
    index: Int,
    output: WalletAbiReviewOutputLook,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_abi_flow_output_$index"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Output ${index + 1}",
            style = labelLarge,
            color = whiteMedium
        )
        Text(
            text = output.address,
            style = MaterialTheme.typography.titleMedium
        )
        ReviewValueColumn(
            primary = output.amount,
            secondary = output.amountFiat
        )
    }
}

@Composable
private fun AssetImpactRow(
    impact: WalletAbiReviewAssetImpactLook,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = impact.assetId,
            style = labelLarge,
            modifier = Modifier.weight(0.6f)
        )
        Text(
            text = impact.walletDelta,
            style = bodyMedium,
            modifier = Modifier.weight(0.4f)
        )
    }
}

@Composable
private fun LegacyRequestLoadedContent(
    state: WalletAbiFlowState.RequestLoaded,
    onIntent: (WalletAbiFlowIntent) -> Unit,
) {
    WalletAbiScreenScaffold(
        body = {
            WalletAbiStateHero(
                title = "Review Wallet ABI request",
                supporting = "This request is loaded without the exact review look. Only approve if the basic wallet details are expected.",
                testTag = "wallet_abi_flow_request_title"
            )
            GreenAlert(
                title = "Limited Wallet ABI review",
                message = "The exact review layout is unavailable, so only basic request facts are shown.",
                modifier = Modifier.testTag("wallet_abi_flow_review_warning")
            )
            Text(state.review.title)
            Text(state.review.message)
            Text("Wallet: ${state.review.requestContext.walletId}")
            Text("Request: ${state.review.requestContext.requestId}")
            state.review.accounts.firstOrNull { it.accountId == state.review.selectedAccountId }?.let { account ->
                Text(
                    text = "Account: ${account.name}",
                    modifier = Modifier.testTag("wallet_abi_flow_selected_account")
                )
            }
            when (val parsedRequest = state.review.parsedRequest) {
                is WalletAbiParsedRequest.TxCreate -> {
                    val request = parsedRequest.request
                    Text(
                        text = "Parsed request: ${request.requestId}",
                        modifier = Modifier.testTag("wallet_abi_flow_parsed_request_id")
                    )
                    Text(
                        text = "Network: ${request.network.wireValue}",
                        modifier = Modifier.testTag("wallet_abi_flow_parsed_network")
                    )
                    state.review.executionDetails?.also { details ->
                        Text(
                            text = "Destination: ${details.destinationAddress}",
                            modifier = Modifier.testTag("wallet_abi_flow_destination")
                        )
                        Text(
                            text = "Amount: ${details.amountSat}",
                            modifier = Modifier.testTag("wallet_abi_flow_amount")
                        )
                        Text(
                            text = "Asset: ${details.assetId}",
                            modifier = Modifier.testTag("wallet_abi_flow_asset")
                        )
                        details.feeRate?.also { feeRate ->
                            Text(
                                text = "Fee rate: $feeRate",
                                modifier = Modifier.testTag("wallet_abi_flow_fee_rate")
                            )
                        }
                    }
                }

                null -> Unit
            }
            state.review.accounts.forEachIndexed { index, account ->
                GreenButton(
                    text = if (account.accountId == state.review.selectedAccountId) {
                        "${account.name} (selected)"
                    } else {
                        account.name
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "wallet_abi_flow_account_$index",
                    type = GreenButtonType.OUTLINE,
                    color = GreenButtonColor.WHITE
                ) {
                    onIntent(WalletAbiFlowIntent.SelectAccount(account.accountId))
                }
            }
        },
        footer = {
            GreenButton(
                text = "Reject request",
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_reject_action",
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE
            ) {
                onIntent(WalletAbiFlowIntent.Reject)
            }
            GreenButton(
                text = if (state.review.approvalTarget is WalletAbiApprovalTarget.Jade) {
                    "Approve with Jade"
                } else {
                    "Approve request"
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "wallet_abi_flow_approve_action"
            ) {
                onIntent(WalletAbiFlowIntent.Approve)
            }
        }
    )
}

@Composable
private fun ReviewValueColumn(
    primary: String,
    secondary: String?,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
        secondary?.also {
            Text(
                text = it,
                color = whiteMedium,
                style = bodyMedium
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = whiteMedium,
            style = labelLarge,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = bodyMedium,
            modifier = Modifier
                .weight(0.6f)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
    }
}

private fun WalletAbiReviewLook.reviewIntro(): String {
    return when (requestFamily) {
        WalletAbiRequestFamily.PAYMENT,
        WalletAbiRequestFamily.SPLIT -> {
            "Review the exact Liquid transaction details before you approve this Wallet ABI request."
        }

        WalletAbiRequestFamily.ISSUANCE -> {
            "Review the exact issuance details before you approve this Wallet ABI request. Issued asset ids, wallet deltas, and fees must match your intent."
        }

        WalletAbiRequestFamily.REISSUANCE -> {
            "Review the exact reissuance details before you approve this Wallet ABI request. Confirm the asset impacts and funding effects carefully."
        }
    }
}

private fun WalletAbiReviewLook.reviewWarningTitle(): String {
    return when (requestFamily) {
        WalletAbiRequestFamily.PAYMENT,
        WalletAbiRequestFamily.SPLIT -> "Approval signs and broadcasts a real Liquid transaction"
        WalletAbiRequestFamily.ISSUANCE -> "Approval resolves, signs, and broadcasts a real Liquid issuance"
        WalletAbiRequestFamily.REISSUANCE -> "Approval resolves, signs, and broadcasts a real Liquid reissuance"
    }
}

private fun WalletAbiReviewLook.reviewWarningBody(): String {
    return when (requestFamily) {
        WalletAbiRequestFamily.PAYMENT,
        WalletAbiRequestFamily.SPLIT ->
            "Check the funding account, each requested recipient, the total fees, and the network before you continue."

        WalletAbiRequestFamily.ISSUANCE ->
            "Check the funding account, issued asset ids, reissuance token details, and wallet deltas before you continue."

        WalletAbiRequestFamily.REISSUANCE ->
            "Check the source token, the reissued asset impact, and the final wallet deltas before you continue."
    }
}

private fun WalletAbiReviewLook.amountSummaryPrimary(): String {
    return outputs.singleOrNull()?.amount ?: "${outputs.size} requested outputs"
}

private fun WalletAbiReviewLook.amountSummarySecondary(): String? {
    return outputs.singleOrNull()?.amountFiat ?: transactionConfirmation?.total
}

private fun WalletAbiReviewLook.assetSummaryPrimary(): String {
    return listOfNotNull(assetTicker, assetName)
        .distinct()
        .joinToString(" · ")
        .ifBlank { assetId }
}

private fun WalletAbiResolutionState.alertTitle(): String {
    return when (this) {
        WalletAbiResolutionState.REQUIRED -> "Resolution required"
        WalletAbiResolutionState.READY -> "Exact review ready"
        WalletAbiResolutionState.NOT_REQUIRED -> "Review status"
    }
}

private fun WalletAbiResumePhase.resumeTitle(): String {
    return when (this) {
        WalletAbiResumePhase.REQUEST_LOADED -> "Resume Wallet ABI request"
        WalletAbiResumePhase.AWAITING_APPROVAL -> "Resume Wallet ABI approval"
        WalletAbiResumePhase.SUBMITTING -> "Wallet ABI request needs attention"
    }
}

private fun WalletAbiResumePhase.resumeBody(): String {
    return when (this) {
        WalletAbiResumePhase.REQUEST_LOADED ->
            "This wallet has a pending Wallet ABI request. Resume to rebuild the current review, or cancel to clear it."

        WalletAbiResumePhase.AWAITING_APPROVAL ->
            "This wallet has a pending Wallet ABI approval. Resume to rebuild the review and re-arm the device approval step."

        WalletAbiResumePhase.SUBMITTING ->
            "This Wallet ABI request was interrupted mid-submission. Resume so the app can explain the safest next step."
    }
}

private fun WalletAbiResumePhase.label(): String {
    return when (this) {
        WalletAbiResumePhase.REQUEST_LOADED -> "Review"
        WalletAbiResumePhase.AWAITING_APPROVAL -> "Awaiting approval"
        WalletAbiResumePhase.SUBMITTING -> "Submitting"
    }
}

private fun WalletAbiCancelledReason.cancelledTitle(): String {
    return when (this) {
        WalletAbiCancelledReason.UserRejected -> "Wallet ABI request rejected"
        WalletAbiCancelledReason.RequestExpired -> "Wallet ABI request expired"
        else -> "Wallet ABI request cancelled"
    }
}

private fun WalletAbiCancelledReason.cancelledBody(): String {
    return when (this) {
        WalletAbiCancelledReason.JadeCancelled ->
            "Jade cancelled the Wallet ABI approval before any transaction could be broadcast."

        WalletAbiCancelledReason.RequestExpired ->
            "The Wallet ABI request expired before it reached a terminal success state."

        WalletAbiCancelledReason.ResumableCancelled ->
            "The saved Wallet ABI request was cleared instead of being resumed."

        WalletAbiCancelledReason.UserCancelled ->
            "You cancelled the Wallet ABI request before it could continue."

        WalletAbiCancelledReason.UserRejected ->
            "You rejected the Wallet ABI request during review, so nothing was approved."
    }
}

private fun WalletAbiCancelledReason.label(): String {
    return when (this) {
        WalletAbiCancelledReason.JadeCancelled -> "Jade approval cancelled"
        WalletAbiCancelledReason.RequestExpired -> "Request expired"
        WalletAbiCancelledReason.ResumableCancelled -> "Pending request cleared"
        WalletAbiCancelledReason.UserCancelled -> "Request cancelled"
        WalletAbiCancelledReason.UserRejected -> "Request rejected"
    }
}

private fun WalletAbiJadeContext.awaitingTitle(): String {
    return when (step) {
        WalletAbiJadeStep.CONNECT -> "Connect Jade to continue"
        WalletAbiJadeStep.UNLOCK -> "Unlock Jade to continue"
        WalletAbiJadeStep.REVIEW -> "Review the Wallet ABI request on Jade"
        WalletAbiJadeStep.SIGN -> "Confirm the signature on Jade"
    }
}

private fun WalletAbiJadeContext.awaitingBody(): String {
    return when (step) {
        WalletAbiJadeStep.CONNECT ->
            "The Wallet ABI request is waiting for Jade so the device can participate in approval."

        WalletAbiJadeStep.UNLOCK ->
            "Unlock Jade and verify that the device is ready before this Wallet ABI request can continue."

        WalletAbiJadeStep.REVIEW ->
            "Check the request details on Jade carefully. Continue only if they match the on-screen Wallet ABI review."

        WalletAbiJadeStep.SIGN ->
            "Approve the signature on Jade only if the final device details match the reviewed Wallet ABI request."
    }
}

private fun WalletAbiJadeStep.label(): String {
    return when (this) {
        WalletAbiJadeStep.CONNECT -> "Connect"
        WalletAbiJadeStep.UNLOCK -> "Unlock"
        WalletAbiJadeStep.REVIEW -> "Review"
        WalletAbiJadeStep.SIGN -> "Sign"
    }
}

private fun WalletAbiFlowState.Submitting.submittingTitle(): String {
    return when {
        stage == WalletAbiSubmittingStage.BROADCASTING ->
            "Broadcasting Wallet ABI transaction"

        isCancelling ->
            "Cancelling Wallet ABI request"

        jade?.step == WalletAbiJadeStep.SIGN ->
            "Preparing Wallet ABI transaction after Jade approval"

        else ->
            "Preparing Wallet ABI transaction"
    }
}

private fun WalletAbiFlowState.Submitting.submittingBody(): String {
    return when {
        stage == WalletAbiSubmittingStage.BROADCASTING ->
            "The app already started broadcasting the reviewed transaction. Wallet or network state may already change, so this step can no longer be cancelled."

        isCancelling ->
            "The app is stopping the Wallet ABI request before broadcast. Wallet state should remain unchanged."

        jade?.step == WalletAbiJadeStep.SIGN ->
            "Jade approval finished. The app is preparing the exact transaction you reviewed, but it has not been broadcast yet."

        else ->
            "The app is preparing the exact Wallet ABI transaction you reviewed. It has not been broadcast yet."
    }
}

private fun WalletAbiSubmittingStage.label(): String {
    return when (this) {
        WalletAbiSubmittingStage.PREPARING -> "Preparing"
        WalletAbiSubmittingStage.BROADCASTING -> "Broadcasting"
    }
}

private fun WalletAbiFlowPhase.label(): String {
    return when (this) {
        WalletAbiFlowPhase.LOADING -> "Loading"
        WalletAbiFlowPhase.APPROVAL -> "Approval"
        WalletAbiFlowPhase.SUBMISSION -> "Submission"
    }
}

private fun WalletAbiRequestFamily.label(): String {
    return when (this) {
        WalletAbiRequestFamily.PAYMENT -> "Payment"
        WalletAbiRequestFamily.SPLIT -> "Split payment"
        WalletAbiRequestFamily.ISSUANCE -> "Issuance"
        WalletAbiRequestFamily.REISSUANCE -> "Reissuance"
    }
}

private fun WalletAbiFlowError.title(): String {
    return when (kind) {
        WalletAbiFlowErrorKind.INVALID_REQUEST -> "Invalid Wallet ABI request"
        WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST -> "Unsupported Wallet ABI request"
        WalletAbiFlowErrorKind.DEVICE_FAILURE -> "Device problem"
        WalletAbiFlowErrorKind.NETWORK_FAILURE -> "Network problem"
        WalletAbiFlowErrorKind.TIMEOUT -> "Wallet ABI request timed out"
        WalletAbiFlowErrorKind.PARTIAL_COMPLETION -> "Transaction status uncertain"
        WalletAbiFlowErrorKind.EXECUTION_FAILURE -> "Wallet ABI execution failed"
    }
}

private fun WalletAbiFlowError.body(): String {
    return when (kind) {
        WalletAbiFlowErrorKind.PARTIAL_COMPLETION ->
            "Transaction status may already have changed. The app will not retry automatically."

        else -> message
    }
}

private val previewRequestContext = WalletAbiStartRequestContext(
    requestId = "wallet-abi-preview-request",
    walletId = "wallet-preview-id"
)

private val previewSoftwareReview = WalletAbiFlowReview(
    requestContext = previewRequestContext,
    method = "wallet_abi_process_request",
    title = "Wallet ABI payment",
    message = "Approve a Wallet ABI request",
    accounts = listOf(
        WalletAbiAccountOption("account-1", "Liquid singlesig"),
        WalletAbiAccountOption("account-2", "Savings")
    ),
    selectedAccountId = "account-1",
    approvalTarget = WalletAbiApprovalTarget.Software,
    parsedRequest = WalletAbiParsedRequest.TxCreate(
        request = WalletAbiTxCreateRequest(
            abiVersion = "wallet-abi-0.1",
            requestId = previewRequestContext.requestId,
            network = WalletAbiNetwork.TESTNET_LIQUID,
            params = WalletAbiRuntimeParams(
                inputs = listOf(
                    WalletAbiInput(
                        id = "input-1",
                        utxoSource = kotlinx.serialization.json.buildJsonObject {
                            put("type", JsonPrimitive("wallet"))
                        },
                        unblinding = kotlinx.serialization.json.buildJsonObject {
                            put("type", JsonPrimitive("wallet"))
                        },
                        sequence = 1L,
                        finalizer = kotlinx.serialization.json.buildJsonObject {
                            put("type", JsonPrimitive("wallet"))
                        }
                    )
                ),
                outputs = listOf(
                    WalletAbiOutput(
                        id = "output-1",
                        amountSat = 120_000L,
                        lock = kotlinx.serialization.json.buildJsonObject {
                            put("type", JsonPrimitive("script"))
                        },
                        asset = kotlinx.serialization.json.buildJsonObject {
                            put("asset_id", JsonPrimitive(TESTNET_POLICY_ASSET))
                        },
                        blinder = kotlinx.serialization.json.buildJsonObject {
                            put("type", JsonPrimitive("rand"))
                        }
                    )
                ),
                feeRateSatKvb = 1200f
            ),
            broadcast = true
        )
    ),
    executionDetails = WalletAbiExecutionDetails(
        destinationAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
        amountSat = 120_000L,
        assetId = TESTNET_POLICY_ASSET,
        network = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
        feeRate = 1_200L
    )
)

private val previewSoftwareReviewLook = WalletAbiReviewLook(
    accountAssetBalance = previewAccountAssetBalance(),
    outputs = listOf(
        WalletAbiReviewOutputLook(
            address = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
            amount = "0.0012 TEST-LBTC",
            amountFiat = "0.12 USD",
            assetId = TESTNET_POLICY_ASSET,
            recipientScript = "00140000000000000000000000000000000000000000"
        )
    ),
    recipientAddress = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
    amount = "0.0012 TEST-LBTC",
    amountFiat = "0.12 USD",
    assetName = "Testnet Liquid Bitcoin",
    assetTicker = "TEST-LBTC",
    assetId = TESTNET_POLICY_ASSET,
    networkName = "Liquid Testnet",
    networkWireValue = WalletAbiNetwork.TESTNET_LIQUID.wireValue,
    method = "wallet_abi_process_request",
    abiVersion = "wallet-abi-0.1",
    requestId = previewRequestContext.requestId,
    broadcast = true,
    recipientScript = "00140000000000000000000000000000000000000000",
    transactionConfirmation = TransactionConfirmation(
        utxos = listOf(
            UtxoView(
                address = "tlq1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3l4q9m",
                assetId = TESTNET_POLICY_ASSET,
                satoshi = 120_000L,
                amount = "0.0012 TEST-LBTC",
                amountExchange = "0.12 USD"
            )
        ),
        fee = "0.000001 TEST-LBTC",
        feeFiat = "0.00 USD",
        feeRate = "12 sat/vB",
        total = "0.001201 TEST-LBTC",
        totalFiat = "0.12 USD"
    )
)

private val previewUnresolvedIssuanceReview = previewSoftwareReview.copy(
    approvalTarget = WalletAbiApprovalTarget.Software,
    executionDetails = previewSoftwareReview.executionDetails?.copy(
        requestFamily = WalletAbiRequestFamily.ISSUANCE,
        resolutionState = WalletAbiResolutionState.REQUIRED,
        outputCount = 2
    )
)

private val previewUnresolvedIssuanceLook = previewSoftwareReviewLook.copy(
    outputs = listOf(
        WalletAbiReviewOutputLook(
            address = "Wallet output",
            amount = "5 units",
            amountFiat = null,
            assetId = "issuance_asset",
            recipientScript = null
        ),
        WalletAbiReviewOutputLook(
            address = "Wallet output",
            amount = "1 unit",
            amountFiat = null,
            assetId = "issuance_token",
            recipientScript = null
        )
    ),
    amount = "2 requested outputs",
    amountFiat = null,
    assetName = "Issued assets",
    assetTicker = null,
    assetId = "deferred",
    transactionConfirmation = null,
    requestFamily = WalletAbiRequestFamily.ISSUANCE,
    resolutionState = WalletAbiResolutionState.REQUIRED,
    statusMessage = "Resolve the request to review exact issued asset ids, wallet deltas, and final fees before approval.",
    warnings = listOf("Issued and reissued asset ids are deferred until resolution completes."),
    canResolve = true,
    canApprove = false
)

private val previewReadyIssuanceLook = previewUnresolvedIssuanceLook.copy(
    outputs = listOf(
        WalletAbiReviewOutputLook(
            address = "Wallet output",
            amount = "5 units",
            amountFiat = null,
            assetId = "issued_asset_id",
            recipientScript = "wallet",
        ),
        WalletAbiReviewOutputLook(
            address = "Wallet output",
            amount = "1 unit",
            amountFiat = null,
            assetId = "reissuance_token_id",
            recipientScript = "wallet",
        )
    ),
    assetName = "Issued asset family",
    assetId = "issued_asset_id",
    transactionConfirmation = previewSoftwareReviewLook.transactionConfirmation,
    resolutionState = WalletAbiResolutionState.READY,
    statusMessage = "Exact Wallet ABI review is ready. Approval will broadcast the resolved transaction.",
    assetImpacts = listOf(
        WalletAbiReviewAssetImpactLook(
            assetId = "issued_asset_id",
            walletDelta = "+5 units"
        ),
        WalletAbiReviewAssetImpactLook(
            assetId = "reissuance_token_id",
            walletDelta = "+1 unit"
        ),
        WalletAbiReviewAssetImpactLook(
            assetId = TESTNET_POLICY_ASSET,
            walletDelta = "-0.000001 TEST-LBTC"
        )
    ),
    canResolve = false,
    canApprove = true
)

@Preview
@Composable
private fun WalletAbiLoadingPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Loading(previewRequestContext),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiRequestLoadedPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.RequestLoaded(previewSoftwareReview),
            onIntent = {},
            reviewLook = previewSoftwareReviewLook
        )
    }
}

@Preview
@Composable
private fun WalletAbiResolutionRequiredPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.RequestLoaded(previewUnresolvedIssuanceReview),
            onIntent = {},
            reviewLook = previewUnresolvedIssuanceLook
        )
    }
}

@Preview
@Composable
private fun WalletAbiResolutionReadyPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.RequestLoaded(previewUnresolvedIssuanceReview),
            onIntent = {},
            reviewLook = previewReadyIssuanceLook
        )
    }
}

@Preview
@Composable
private fun WalletAbiAwaitingApprovalPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.AwaitingApproval(
                requestContext = previewRequestContext,
                selectedAccountId = "account-1",
                jade = WalletAbiJadeContext(
                    deviceId = "jade-id",
                    step = WalletAbiJadeStep.REVIEW,
                    message = "Confirm the transaction details on Jade.",
                    retryable = false
                )
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiSubmittingPreparingPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Submitting(
                requestContext = previewRequestContext,
                stage = WalletAbiSubmittingStage.PREPARING
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiSubmittingBroadcastingPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Submitting(
                requestContext = previewRequestContext,
                stage = WalletAbiSubmittingStage.BROADCASTING
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiSuccessPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Success(
                WalletAbiSuccessResult(
                    requestId = previewRequestContext.requestId,
                    txHash = "1f0a25c0f36e3fb5ff2a3e06ab9a9f9f9a4d50f0aab0ccdd11aa22bb33cc44dd",
                    responseId = null
                )
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiCancelledPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiErrorPreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Error(
                WalletAbiFlowError(
                    kind = WalletAbiFlowErrorKind.PARTIAL_COMPLETION,
                    phase = WalletAbiFlowPhase.SUBMISSION,
                    message = "ignored",
                    retryable = false
                )
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun WalletAbiResumablePreview() {
    GreenPreview {
        WalletAbiFlowScreen(
            state = WalletAbiFlowState.Resumable(
                WalletAbiResumeSnapshot(
                    review = previewSoftwareReview,
                    phase = WalletAbiResumePhase.REQUEST_LOADED
                )
            ),
            onIntent = {}
        )
    }
}

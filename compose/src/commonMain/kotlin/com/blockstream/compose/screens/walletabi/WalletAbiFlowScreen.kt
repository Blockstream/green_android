package com.blockstream.compose.screens.walletabi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.TransactionConfirmationSummary
import com.blockstream.compose.models.walletabi.WalletAbiFlowRouteViewModel
import com.blockstream.compose.models.walletabi.WalletAbiFlowViewModel
import com.blockstream.compose.models.walletabi.WalletAbiReviewLook
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiJadeEvent
import com.blockstream.domain.walletabi.flow.WalletAbiJadeStep
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import org.jetbrains.compose.resources.stringResource

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Wallet ABI",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("wallet_abi_flow_title")
            )

            when (state) {
                WalletAbiFlowState.Idle -> {
                    Text("No active Wallet ABI request")
                }

                is WalletAbiFlowState.Loading -> {
                    Text(
                        text = "Loading Wallet ABI request ${state.requestContext.requestId}",
                        modifier = Modifier.testTag("wallet_abi_flow_loading")
                    )
                }

                is WalletAbiFlowState.RequestLoaded -> {
                    RequestLoadedContent(
                        state = state,
                        reviewLook = reviewLook,
                        onIntent = onIntent
                    )
                }

                is WalletAbiFlowState.AwaitingApproval -> {
                    Text(
                        text = "Awaiting Jade approval",
                        modifier = Modifier.testTag("wallet_abi_flow_awaiting_approval")
                    )
                    Text("Step: ${state.jade.step.name.lowercase()}")
                    GreenButton(
                        text = "Cancel approval",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "wallet_abi_flow_jade_cancel_action"
                    ) {
                        onIntent(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Cancelled))
                    }
                }

                is WalletAbiFlowState.Submitting -> {
                    Text(
                        text = if (state.jade?.step == WalletAbiJadeStep.SIGN) {
                            "Submitting Wallet ABI request after Jade signing"
                        } else {
                            "Submitting Wallet ABI request"
                        },
                        modifier = Modifier.testTag("wallet_abi_flow_submitting")
                    )
                }

                is WalletAbiFlowState.Success -> {
                    Text(
                        text = "Request completed",
                        modifier = Modifier.testTag("wallet_abi_flow_success")
                    )
                    Text(
                        text = "Transaction: ${state.result.txHash ?: state.result.responseId ?: "unavailable"}",
                        modifier = Modifier.testTag("wallet_abi_flow_success_tx_hash")
                    )
                    GreenButton(
                        text = "Done",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "wallet_abi_flow_terminal_dismiss_action"
                    ) {
                        onIntent(WalletAbiFlowIntent.DismissTerminal)
                    }
                }

                is WalletAbiFlowState.Cancelled -> {
                    Text(
                        text = "Flow cancelled: ${state.reason.label()}",
                        modifier = Modifier.testTag("wallet_abi_flow_cancelled")
                    )
                    GreenButton(
                        text = "Done",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "wallet_abi_flow_terminal_dismiss_action"
                    ) {
                        onIntent(WalletAbiFlowIntent.DismissTerminal)
                    }
                }

                is WalletAbiFlowState.Error -> {
                    Text(
                        text = state.error.message,
                        modifier = Modifier.testTag("wallet_abi_flow_error")
                    )
                    GreenButton(
                        text = "Retry",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "wallet_abi_flow_retry_action"
                    ) {
                        onIntent(WalletAbiFlowIntent.Retry)
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

                is WalletAbiFlowState.Resumable -> {
                    Text(
                        text = "Resume Wallet ABI request ${state.snapshot.review.requestContext.requestId}",
                        modifier = Modifier.testTag("wallet_abi_flow_resumable")
                    )
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
            }
        }
    }
}

@Composable
private fun ColumnScope.RequestLoadedContent(
    state: WalletAbiFlowState.RequestLoaded,
    reviewLook: WalletAbiReviewLook?,
    onIntent: (WalletAbiFlowIntent) -> Unit
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
        "Approve request"
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.id_review),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("wallet_abi_flow_request_title")
        )
        Text(
            text = "Review the exact Liquid transaction details before you approve this Wallet ABI request.",
            style = bodyMedium,
            color = whiteMedium
        )

        GreenAlert(
            title = "Real Liquid payment request",
            message = "Check the sending account, recipient address, amount, and fees. Approval will sign and broadcast a real transaction.",
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
                title = "Use account",
                testTag = "wallet_abi_flow_account_switcher"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .padding(16.dp, if (index == 0) 16.dp else 0.dp, 16.dp, 0.dp),
                            type = GreenButtonType.OUTLINE,
                            color = GreenButtonColor.WHITE,
                            testTag = "wallet_abi_flow_account_$index"
                        ) {
                            onIntent(WalletAbiFlowIntent.SelectAccount(account.accountId))
                        }
                    }
                }
            }
        }

        GreenAmount(
            title = stringResource(Res.string.id_recipient_address),
            amount = reviewLook.amount,
            amountFiat = reviewLook.amountFiat,
            address = reviewLook.recipientAddress,
            modifier = Modifier.testTag("wallet_abi_flow_destination")
        )

        GreenDataLayout(
            title = stringResource(Res.string.id_amount),
            testTag = "wallet_abi_flow_amount_summary"
        ) {
            ReviewValueColumn(
                primary = reviewLook.amount,
                secondary = reviewLook.amountFiat,
                modifier = Modifier.padding(16.dp)
            )
        }

        GreenDataLayout(
            title = "Asset",
            testTag = "wallet_abi_flow_asset_summary"
        ) {
            ReviewValueColumn(
                primary = listOfNotNull(reviewLook.assetTicker, reviewLook.assetName)
                    .distinct()
                    .joinToString(" · ")
                    .ifBlank { reviewLook.assetId },
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
                TransactionConfirmationSummary(
                    confirmation = reviewLook.transactionConfirmation
                )
            }
        }

        if (state.review.approvalTarget is WalletAbiApprovalTarget.Jade) {
            Text(
                text = "Approval continues on Jade after this review.",
                style = bodySmall,
                color = whiteMedium
            )
        }

        AnimatedVisibility(visible = reviewLook.isRefreshing) {
            Text(
                text = "Refreshing the exact transaction preview for the selected account.",
                style = bodySmall,
                color = whiteMedium,
                modifier = Modifier.testTag("wallet_abi_flow_review_refreshing")
            )
        }

        GreenButton(
            text = if (technicalDetailsExpanded) "Hide request details" else "Show request details",
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
                    DetailRow("Request ID", reviewLook.requestId, testTag = "wallet_abi_flow_parsed_request_id")
                    DetailRow("Broadcast", reviewLook.broadcast.toString())
                    DetailRow("Network", reviewLook.networkWireValue, testTag = "wallet_abi_flow_parsed_network")
                    DetailRow("Selected account", state.review.selectedAccountId ?: "n/a")
                    DetailRow("Asset ID", reviewLook.assetId, testTag = "wallet_abi_flow_asset")
                    reviewLook.transactionConfirmation.feeRate?.also { feeRate ->
                        DetailRow("Fee rate", feeRate, testTag = "wallet_abi_flow_fee_rate")
                    }
                    reviewLook.recipientScript?.also { script ->
                        DetailRow("Recipient script", script)
                    }
                }
            }
        }
    }

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
    GreenButton(
        text = approvalText,
        modifier = Modifier.fillMaxWidth(),
        testTag = "wallet_abi_flow_approve_action",
        enabled = !reviewLook.isRefreshing,
        onProgress = reviewLook.isRefreshing,
        size = GreenButtonSize.LARGE
    ) {
        onIntent(WalletAbiFlowIntent.Approve)
    }
}

@Composable
private fun LegacyRequestLoadedContent(
    state: WalletAbiFlowState.RequestLoaded,
    onIntent: (WalletAbiFlowIntent) -> Unit
) {
    Text(state.review.title, modifier = Modifier.testTag("wallet_abi_flow_request_title"))
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
            modifier = Modifier
                .fillMaxWidth(),
            testTag = "wallet_abi_flow_account_$index",
            type = GreenButtonType.OUTLINE,
            color = GreenButtonColor.WHITE
        ) {
            onIntent(WalletAbiFlowIntent.SelectAccount(account.accountId))
        }
    }
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

@Composable
private fun ReviewValueColumn(
    primary: String,
    secondary: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("wallet_abi_flow_amount")
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
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

private fun WalletAbiCancelledReason.label(): String {
    return when (this) {
        WalletAbiCancelledReason.JadeCancelled -> "Jade approval cancelled"
        WalletAbiCancelledReason.RequestExpired -> "Request expired"
        WalletAbiCancelledReason.ResumableCancelled -> "Resumed request cancelled"
        WalletAbiCancelledReason.UserRejected -> "User rejected request"
    }
}

package com.blockstream.compose.screens.walletabi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.compose.models.walletabi.WalletAbiFlowViewModel
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiJadeStep

@Composable
fun WalletAbiFlowScreen(viewModel: WalletAbiFlowViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WalletAbiFlowScreen(
        state = state,
        onIntent = viewModel::dispatch
    )
}

@Composable
fun WalletAbiFlowScreen(
    state: WalletAbiFlowState,
    onIntent: (WalletAbiFlowIntent) -> Unit
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
                    Text(state.review.title, modifier = Modifier.testTag("wallet_abi_flow_request_title"))
                    Text(state.review.message)
                    Text("Wallet: ${state.review.requestContext.walletId}")
                    Text("Request: ${state.review.requestContext.requestId}")
                    state.review.accounts.forEachIndexed { index, account ->
                        OutlinedButton(
                            onClick = { onIntent(WalletAbiFlowIntent.SelectAccount(account.accountId)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_abi_flow_account_$index")
                        ) {
                            Text(
                                if (account.accountId == state.review.selectedAccountId) {
                                    "${account.name} (selected)"
                                } else {
                                    account.name
                                }
                            )
                        }
                    }
                    Text(
                        when (state.review.approvalTarget) {
                            is WalletAbiApprovalTarget.Jade -> "Approval target: Jade"
                            WalletAbiApprovalTarget.Software -> "Approval target: Software"
                        }
                    )
                    OutlinedButton(
                        onClick = { onIntent(WalletAbiFlowIntent.ResolveRequest) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_resolve_action")
                    ) {
                        Text("Resolve request")
                    }
                    OutlinedButton(
                        onClick = { onIntent(WalletAbiFlowIntent.Reject) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_reject_action")
                    ) {
                        Text("Reject request")
                    }
                    Button(
                        onClick = { onIntent(WalletAbiFlowIntent.Approve) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_approve_action")
                    ) {
                        Text(
                            if (state.review.approvalTarget is WalletAbiApprovalTarget.Jade) {
                                "Approve with Jade"
                            } else {
                                "Approve request"
                            }
                        )
                    }
                }

                is WalletAbiFlowState.AwaitingApproval -> {
                    Text(
                        text = "Awaiting Jade approval",
                        modifier = Modifier.testTag("wallet_abi_flow_awaiting_approval")
                    )
                    Text("Step: ${state.jade.step.name.lowercase()}")
                    Button(
                        onClick = { onIntent(WalletAbiFlowIntent.OnJadeEvent(com.blockstream.domain.walletabi.flow.WalletAbiJadeEvent.Cancelled)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_jade_cancel_action")
                    ) {
                        Text("Cancel approval")
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
                    Text("Response: ${state.result.responseId}")
                    Button(
                        onClick = { onIntent(WalletAbiFlowIntent.DismissTerminal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_terminal_dismiss_action")
                    ) {
                        Text("Done")
                    }
                }

                is WalletAbiFlowState.Cancelled -> {
                    Text(
                        text = "Flow cancelled: ${state.reason.label()}",
                        modifier = Modifier.testTag("wallet_abi_flow_cancelled")
                    )
                    Button(
                        onClick = { onIntent(WalletAbiFlowIntent.DismissTerminal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_terminal_dismiss_action")
                    ) {
                        Text("Done")
                    }
                }

                is WalletAbiFlowState.Error -> {
                    Text(
                        text = state.error.message,
                        modifier = Modifier.testTag("wallet_abi_flow_error")
                    )
                    Button(
                        onClick = { onIntent(WalletAbiFlowIntent.Retry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_retry_action")
                    ) {
                        Text("Retry")
                    }
                    OutlinedButton(
                        onClick = { onIntent(WalletAbiFlowIntent.DismissTerminal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_terminal_dismiss_action")
                    ) {
                        Text("Dismiss")
                    }
                }

                is WalletAbiFlowState.Resumable -> {
                    Text(
                        text = "Resume Wallet ABI request ${state.snapshot.review.requestContext.requestId}",
                        modifier = Modifier.testTag("wallet_abi_flow_resumable")
                    )
                    Button(
                        onClick = { onIntent(WalletAbiFlowIntent.Resume) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_resume_action")
                    ) {
                        Text("Resume flow")
                    }
                    OutlinedButton(
                        onClick = { onIntent(WalletAbiFlowIntent.CancelResume) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_abi_flow_cancel_resume_action")
                    ) {
                        Text("Cancel flow")
                    }
                }
            }
        }
    }
}

private fun WalletAbiCancelledReason.label(): String = when (this) {
    WalletAbiCancelledReason.JadeCancelled -> "jade_cancelled"
    WalletAbiCancelledReason.RequestExpired -> "request_expired"
    WalletAbiCancelledReason.ResumableCancelled -> "resumable_cancelled"
    WalletAbiCancelledReason.UserRejected -> "user_rejected"
}

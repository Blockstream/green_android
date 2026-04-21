package com.blockstream.compose.screens.walletabi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.models.walletabi.WalletAbiWalletConnectAwaitingTransportLook
import com.blockstream.compose.models.walletabi.WalletAbiWalletConnectConnectedLook
import com.blockstream.compose.models.walletabi.WalletAbiWalletConnectConnectionLook
import com.blockstream.compose.models.walletabi.WalletAbiWalletConnectRouteViewModel
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState

@Composable
fun WalletAbiWalletConnectScreen(
    viewModel: WalletAbiWalletConnectRouteViewModel,
) {
    val flowState by viewModel.flowState.collectAsStateWithLifecycle()
    val reviewLook by viewModel.reviewLook.collectAsStateWithLifecycle()
    val connectionApproval by viewModel.connectionApproval.collectAsStateWithLifecycle()
    val awaitingTransport by viewModel.awaitingTransport.collectAsStateWithLifecycle()
    val connectedSession by viewModel.connectedSession.collectAsStateWithLifecycle()

    when {
        connectionApproval != null -> WalletAbiWalletConnectConnectionApprovalScreen(
            look = connectionApproval!!,
            onApprove = { viewModel.dispatch(WalletAbiFlowIntent.Approve) },
            onReject = { viewModel.dispatch(WalletAbiFlowIntent.Reject) },
        )

        awaitingTransport != null -> WalletAbiWalletConnectAwaitingTransportScreen(look = awaitingTransport!!)

        flowState !is WalletAbiFlowState.Idle -> WalletAbiFlowScreen(
            state = flowState,
            onIntent = viewModel::dispatch,
            reviewLook = reviewLook,
        )

        connectedSession != null -> WalletAbiWalletConnectConnectedScreen(
            look = connectedSession!!,
            onDisconnect = { viewModel.dispatch(WalletAbiFlowIntent.Cancel) },
        )

        else -> WalletAbiWalletConnectEmptyScreen()
    }
}

@Composable
private fun WalletAbiWalletConnectConnectionApprovalScreen(
    look: WalletAbiWalletConnectConnectionLook,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    WalletAbiWalletConnectScaffold(
        body = {
            WalletAbiWalletConnectHeader(
                title = "Review WalletConnect session",
                supporting = "Approve only if you trust this app to send Wallet ABI transfer and split requests.",
                testTag = "wallet_connect_connection_approval",
            )

            GreenAlert(
                title = if (look.awaitingTransport) {
                    "Waiting for WalletConnect"
                } else {
                    "Connection approval required"
                },
                message = if (look.awaitingTransport) {
                    "Green already approved the connection locally and is waiting for WalletConnect transport confirmation."
                } else {
                    "This app is requesting a WalletConnect session to your current Liquid wallet."
                },
                isBlue = true,
            )

            GreenDataLayout(title = "App", testTag = "wallet_connect_connection_peer") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailRow(label = "Name", value = look.proposal.name)
                    look.proposal.url?.takeIf { it.isNotBlank() }?.let { url ->
                        DetailRow(label = "URL", value = url)
                    }
                    look.proposal.description?.takeIf { it.isNotBlank() }?.let { description ->
                        DetailRow(label = "Description", value = description)
                    }
                }
            }

            GreenDataLayout(title = "WalletConnect scope", testTag = "wallet_connect_connection_scope") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailRow(label = "Chain", value = look.chainId)
                    DetailRow(
                        label = "Methods",
                        value = look.methods.joinToString().ifBlank { "No methods requested" },
                    )
                    if (look.queuedOverlayCount > 0) {
                        DetailRow(label = "Queued requests", value = look.queuedOverlayCount.toString())
                    }
                }
            }
        },
        footer = {
            GreenButton(
                text = if (look.awaitingTransport) "Waiting for WalletConnect" else "Approve session",
                modifier = Modifier.fillMaxWidth(),
                enabled = !look.awaitingTransport,
                testTag = "wallet_connect_connection_approve",
            ) {
                onApprove()
            }
            GreenButton(
                text = "Reject session",
                modifier = Modifier.fillMaxWidth(),
                enabled = !look.awaitingTransport,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE,
                testTag = "wallet_connect_connection_reject",
            ) {
                onReject()
            }
        },
    )
}

@Composable
private fun WalletAbiWalletConnectAwaitingTransportScreen(
    look: WalletAbiWalletConnectAwaitingTransportLook,
) {
    WalletAbiWalletConnectScaffold(
        body = {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp),
            )
            WalletAbiWalletConnectHeader(
                title = "Waiting for WalletConnect transport",
                supporting = "Green already prepared the Wallet ABI response and is waiting for WalletConnect to confirm delivery.",
                testTag = "wallet_connect_awaiting_transport",
            )
            GreenAlert(
                title = "Do not retry yet",
                message = "The paired app may already be receiving the result. Wait for WalletConnect to finish before taking further action.",
                isBlue = true,
            )
            GreenDataLayout(title = "Pending response", testTag = "wallet_connect_awaiting_transport_request") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailRow(label = "Request ID", value = look.requestId)
                    DetailRow(label = "Chain", value = look.chainId)
                    look.peerName?.takeIf { it.isNotBlank() }?.let { peerName ->
                        DetailRow(label = "Peer", value = peerName)
                    }
                }
            }
        },
    )
}

@Composable
private fun WalletAbiWalletConnectConnectedScreen(
    look: WalletAbiWalletConnectConnectedLook,
    onDisconnect: () -> Unit,
) {
    WalletAbiWalletConnectScaffold(
        body = {
            WalletAbiWalletConnectHeader(
                title = look.session.peerName?.takeIf { it.isNotBlank() } ?: "WalletConnect connected",
                supporting = "The session is active. New Wallet ABI transfer or split requests will appear here for review.",
                testTag = "wallet_connect_connected",
            )

            GreenAlert(
                title = if (look.awaitingTransport) {
                    "Waiting for WalletConnect"
                } else {
                    "Ready for requests"
                },
                message = if (look.awaitingTransport) {
                    "Green is still waiting for WalletConnect to confirm the latest action."
                } else {
                    "There is no pending Wallet ABI request right now."
                },
                isBlue = true,
            )

            GreenDataLayout(title = "Session", testTag = "wallet_connect_session") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailRow(label = "Chain", value = look.session.chainId)
                    DetailRow(
                        label = "Methods",
                        value = look.session.methods.joinToString().ifBlank { "No methods registered" },
                    )
                    look.session.peerUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        DetailRow(label = "URL", value = url)
                    }
                }
            }
        },
        footer = {
            GreenButton(
                text = "Disconnect session",
                modifier = Modifier.fillMaxWidth(),
                enabled = !look.awaitingTransport,
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.WHITE,
                testTag = "wallet_connect_disconnect",
            ) {
                onDisconnect()
            }
        },
    )
}

@Composable
private fun WalletAbiWalletConnectEmptyScreen() {
    WalletAbiWalletConnectScaffold(
        body = {
            WalletAbiWalletConnectHeader(
                title = "No active WalletConnect session",
                supporting = "Use the Transact tab to scan or paste a WalletConnect pairing URI, or open a `wc:` link from another app.",
                testTag = "wallet_connect_empty",
            )
            GreenAlert(
                title = "WalletConnect is idle",
                message = "Once a paired app sends a Wallet ABI request, Green will show the exact review here before anything is approved.",
                isBlue = true,
            )
        },
    )
}

@Composable
private fun WalletAbiWalletConnectScaffold(
    body: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = body,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = footer,
            )
        }
    }
}

@Composable
private fun WalletAbiWalletConnectHeader(
    title: String,
    supporting: String,
    testTag: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(testTag),
        )
        Text(
            text = supporting,
            style = bodyMedium,
            color = whiteMedium,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = whiteMedium,
        )
        Text(
            text = value,
            style = bodyMedium,
        )
    }
}

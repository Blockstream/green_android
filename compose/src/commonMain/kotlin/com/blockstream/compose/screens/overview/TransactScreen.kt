package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_latest_transactions
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.components.TransactionActionButtons
import com.blockstream.compose.components.WalletBalance
import com.blockstream.compose.events.Events
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.models.overview.TransactViewModelAbstract
import com.blockstream.compose.models.overview.TransactViewModelPreview
import com.blockstream.compose.models.overview.WalletAbiWalletConnectCardLook
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.SwapUtils
import com.blockstream.compose.utils.bottom
import com.blockstream.compose.utils.plus
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.ScanResult
import com.blockstream.domain.walletabi.flow.WalletAbiResumePhase
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TransactScreen(viewModel: TransactViewModelAbstract) {
    NavigateDestinations.Camera.getResult<ScanResult> {
        viewModel.handleWalletConnectInput(it.result)
    }

    NavigateDestinations.Login.getResult<GreenWallet> {
        SwapUtils.navigateToDeviceScanOrJadeQr(viewModel)
    }

    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false) {

        val platformManager = LocalPlatformManager.current
        val isMainnet = viewModel.greenWallet.isMainnet
        val isSwapAvailable = viewModel.isSwapAvailable
        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        val pendingWalletAbiResume by viewModel.pendingWalletAbiResume.collectAsStateWithLifecycle()
        val walletAbiWalletConnectCard by viewModel.walletAbiWalletConnectCard.collectAsStateWithLifecycle()
        val hasPendingWalletAbiWalletConnectRequest by viewModel.hasPendingWalletAbiWalletConnectRequest.collectAsStateWithLifecycle()
        val isMultisigWatchOnly by viewModel.isMultisigWatchOnly.collectAsStateWithLifecycle()
        val innerPadding = LocalInnerPadding.current
        val listState = rememberLazyListState()
        var lastAutoOpenedWalletConnectKey by rememberSaveable { mutableStateOf<String?>(null) }
        val walletConnectAutoOpenKey = walletAbiWalletConnectCard
            ?.takeIf { hasPendingWalletAbiWalletConnectRequest && it.statusLabel == "Review" }
            ?.let { "${it.title}|${it.subtitle.orEmpty()}|${it.body}" }

        LaunchedEffect(walletConnectAutoOpenKey) {
            when {
                walletConnectAutoOpenKey == null -> lastAutoOpenedWalletConnectKey = null
                walletConnectAutoOpenKey != lastAutoOpenedWalletConnectKey -> {
                    lastAutoOpenedWalletConnectKey = walletConnectAutoOpenKey
                    viewModel.openWalletAbiWalletConnect()
                }
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = innerPadding.bottom()
                .plus(PaddingValues(horizontal = 16.dp))
                .plus(PaddingValues(bottom = 80.dp + 16.dp)),
        ) {

            item(key = "WalletBalance") {
                WalletBalance(viewModel = viewModel)
            }

            item(key = "ButtonsRow") {
                TransactionActionButtons(
                    modifier = Modifier.padding(top = 16.dp),
                    showBuyButton = isMainnet,
                    showSwapButton = isSwapAvailable,
                    isSendEnabled = !isMultisigWatchOnly,
                    onBuy = viewModel::onBuy,
                    onSend = viewModel::onSend,
                    onReceive = viewModel::onReceive,
                    onSwap = viewModel::onSwap
                )
            }

            item(key = "WalletAbiEntry") {
                WalletAbiTransactEntry(
                    isDevelopment = viewModel.appInfo.isDevelopment,
                    pendingSnapshot = pendingWalletAbiResume,
                    walletConnectCard = walletAbiWalletConnectCard,
                    hasPendingWalletConnectRequest = hasPendingWalletAbiWalletConnectRequest,
                    onScanWalletConnect = {
                        viewModel.postEvent(
                            NavigateDestinations.Camera(
                                isDecodeContinuous = true,
                                parentScreenName = viewModel.screenName()
                            )
                        )
                    },
                    onPasteWalletConnect = {
                        platformManager.getClipboard()?.let(viewModel::handleWalletConnectInput)
                    },
                    onOpenWalletConnect = viewModel::openWalletAbiWalletConnect,
                    onOpenDemo = viewModel::openWalletAbiFlow,
                    onResumePending = viewModel::resumePendingWalletAbiFlow
                )
            }

            item(key = "TransactionsHeader") {
                ListHeader(title = stringResource(Res.string.id_latest_transactions))

                if (transactions.isLoading()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .padding(all = 32.dp)
                            .height(1.dp)
                            .fillMaxWidth()
                    )
                } else if (transactions.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.id_your_transactions_will_be_shown),
                        style = bodyMedium,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            items(items = transactions.data() ?: listOf(), key = { tx ->
                tx.transaction.uniqueId
            }) { item ->
                GreenTransaction(modifier = Modifier.padding(bottom = 6.dp), transactionLook = item) {
                    viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                }
            }
        }
    }
}

@Composable
fun WalletAbiTransactEntry(
    isDevelopment: Boolean,
    pendingSnapshot: WalletAbiResumeSnapshot?,
    walletConnectCard: WalletAbiWalletConnectCardLook?,
    hasPendingWalletConnectRequest: Boolean,
    onScanWalletConnect: () -> Unit,
    onPasteWalletConnect: () -> Unit,
    onOpenWalletConnect: () -> Unit,
    onOpenDemo: () -> Unit,
    onResumePending: () -> Unit
) {
    Row(modifier = Modifier.padding(top = 16.dp)) {
        OutlinedButton(
            onClick = onScanWalletConnect,
            modifier = Modifier
                .weight(1f)
                .testTag("transact_wallet_connect_scan")
        ) {
            Text("Scan WalletConnect")
        }

        OutlinedButton(
            onClick = onPasteWalletConnect,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
                .testTag("transact_wallet_connect_paste")
        ) {
            Text("Paste WalletConnect")
        }
    }

    walletConnectCard?.also { card ->
        WalletAbiWalletConnectEntryCard(
            card = card,
            hasPendingWalletConnectRequest = hasPendingWalletConnectRequest,
            onOpen = onOpenWalletConnect
        )
    }

    if (pendingSnapshot != null) {
        WalletAbiPendingResumeEntry(
            snapshot = pendingSnapshot,
            onResume = onResumePending
        )
    }

    WalletAbiDevelopmentEntry(
        visible = isDevelopment && pendingSnapshot == null,
        onOpen = onOpenDemo
    )
}

@Composable
fun WalletAbiWalletConnectEntryCard(
    card: WalletAbiWalletConnectCardLook,
    hasPendingWalletConnectRequest: Boolean,
    onOpen: () -> Unit,
) {
    OutlinedButton(
        onClick = onOpen,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .testTag(
                if (hasPendingWalletConnectRequest) {
                    "transact_wallet_connect_request_card"
                } else {
                    "transact_wallet_connect_card"
                }
            )
    ) {
        Text(
            buildString {
                append(card.title)
                card.subtitle?.takeIf { it.isNotBlank() }?.let {
                    append(" · ")
                    append(it)
                }
                append(" · ")
                append(card.statusLabel)
            }
        )
    }

    Text(
        text = card.body,
        style = bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun WalletAbiPendingResumeEntry(
    snapshot: WalletAbiResumeSnapshot,
    onResume: () -> Unit
) {
    val label = when (snapshot.phase) {
        WalletAbiResumePhase.REQUEST_LOADED,
        WalletAbiResumePhase.AWAITING_APPROVAL -> "Resume Wallet ABI request"

        WalletAbiResumePhase.SUBMITTING -> "Wallet ABI request needs attention"
    }

    OutlinedButton(
        onClick = onResume,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .testTag("transact_wallet_abi_resume_entry")
    ) {
        Text("$label ${snapshot.review.requestContext.requestId}")
    }
}

@Composable
fun WalletAbiDevelopmentEntry(
    visible: Boolean,
    onOpen: () -> Unit
) {
    if (!visible) return

    OutlinedButton(
        onClick = onOpen,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .testTag("transact_wallet_abi_entry")
    ) {
        Text("Open Wallet ABI demo")
    }
}

@Preview
@Composable
fun PreviewTransactScreen() {
    GreenPreview {
        TransactScreen(viewModel = TransactViewModelPreview.create())
    }
}

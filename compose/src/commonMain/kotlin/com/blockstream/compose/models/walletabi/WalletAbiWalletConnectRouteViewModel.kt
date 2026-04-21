package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.json.DefaultJson
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectActionOutcome
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectState
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
import com.blockstream.domain.walletabi.flow.WalletAbiRequestFamily
import com.blockstream.domain.walletabi.flow.WalletAbiResolutionState
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSubmittingStage
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewOutputKind
import com.blockstream.domain.walletabi.provider.WalletAbiProviderProcessResponse
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRequestPreview
import com.blockstream.domain.walletabi.provider.WalletAbiProviderStatus
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiMethod
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lwk.WalletAbiWalletConnectOverlay
import lwk.WalletAbiWalletConnectOverlayKind
import lwk.WalletAbiWalletConnectSessionInfo
import lwk.WalletAbiWalletConnectSessionProposal
import kotlin.math.absoluteValue
import org.koin.core.component.inject

private const val USER_REJECTED_MESSAGE = "wallet connect request rejected by user"
private const val WALLET_ABI_PROCESS_REQUEST_METHOD = "wallet_abi_process_request"

data class WalletAbiWalletConnectConnectionLook(
    val proposal: WalletAbiWalletConnectSessionProposal,
    val chainId: String,
    val methods: List<String>,
    val awaitingTransport: Boolean,
    val queuedOverlayCount: Int,
)

data class WalletAbiWalletConnectAwaitingTransportLook(
    val requestId: String,
    val peerName: String?,
    val chainId: String,
)

data class WalletAbiWalletConnectConnectedLook(
    val session: WalletAbiWalletConnectSessionInfo,
    val awaitingTransport: Boolean,
)

data class WalletAbiWalletConnectPreparingLook(
    val requestId: String,
    val peerName: String?,
    val chainId: String,
)

sealed interface WalletAbiWalletConnectScreenState {
    data object Empty : WalletAbiWalletConnectScreenState

    data object Pairing : WalletAbiWalletConnectScreenState

    data class ConnectionApproval(
        val look: WalletAbiWalletConnectConnectionLook,
    ) : WalletAbiWalletConnectScreenState

    data class Preparing(
        val look: WalletAbiWalletConnectPreparingLook,
    ) : WalletAbiWalletConnectScreenState

    data class WalletAbiFlow(
        val state: WalletAbiFlowState,
        val reviewLook: WalletAbiReviewLook?,
    ) : WalletAbiWalletConnectScreenState

    data class AwaitingTransport(
        val look: WalletAbiWalletConnectAwaitingTransportLook,
    ) : WalletAbiWalletConnectScreenState

    data class Connected(
        val look: WalletAbiWalletConnectConnectedLook,
    ) : WalletAbiWalletConnectScreenState
}

class WalletAbiWalletConnectRouteViewModel(
    greenWallet: GreenWallet,
    private val initialPairingUri: String? = null,
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    private val mutableScreenState =
        MutableStateFlow<WalletAbiWalletConnectScreenState>(WalletAbiWalletConnectScreenState.Empty)
    private val overlayActionMutex = Mutex()

    private var terminalOverride: WalletAbiFlowState? = null
    private var requestApprovalInFlight = false
    private var leaveRouteAfterTerminalDismiss = false
    private var pairingInFlight = false

    override fun screenName(): String = "WalletAbiWalletConnect"

    val screenState: StateFlow<WalletAbiWalletConnectScreenState> = mutableScreenState.asStateFlow()

    init {
        _navData.value = NavData(
            title = "WalletConnect",
            subtitle = greenWallet.name,
        )

        walletAbiWalletConnectManager.state(greenWallet.id)
            .onEach { state ->
                if (shouldPreemptTerminal(state)) {
                    terminalOverride = null
                }
                if (terminalOverride == null) {
                    synchronizeFromManager(state)
                }
            }
            .launchIn(this)

        val pairingUri = initialPairingUri?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            if (pairingUri != null) {
                pairFromRoute(pairingUri)
            } else {
                walletAbiWalletConnectManager.bind(
                    greenWallet = greenWallet,
                    session = session,
                )
            }
        }

        sessionManager.pendingUri
            .filterNotNull()
            .debounce(50L)
            .onEach { uri ->
                if (!uri.trim().startsWith("wc:")) {
                    return@onEach
                }
                sessionManager.consumePendingUri(uri)?.also { pendingUri ->
                    pairFromRoute(pendingUri)
                }
            }
            .launchIn(this)

        bootstrap()
    }

    private suspend fun pairFromRoute(input: String) {
        val pairingInput = input.trim()
        if (pairingInput.isBlank()) {
            return
        }

        pairingInFlight = true
        mutableScreenState.value = WalletAbiWalletConnectScreenState.Pairing
        runCatching {
            walletAbiWalletConnectManager.pair(
                greenWallet = greenWallet,
                session = session,
                input = pairingInput,
            )
        }.onFailure { error ->
            pairingInFlight = false
            mutableScreenState.value = WalletAbiWalletConnectScreenState.WalletAbiFlow(
                state = WalletAbiFlowState.Error(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                        phase = WalletAbiFlowPhase.LOADING,
                        message = error.message ?: "WalletConnect pairing failed",
                        retryable = true,
                    )
                ),
                reviewLook = null,
            )
        }
    }

    fun dispatch(intent: WalletAbiFlowIntent) {
        viewModelScope.launch {
            runCatching {
                when (intent) {
                    WalletAbiFlowIntent.Approve -> handleApprove()
                    WalletAbiFlowIntent.Reject -> handleReject()
                    WalletAbiFlowIntent.Cancel -> handleCancel()
                    WalletAbiFlowIntent.DismissTerminal -> dismissTerminal()
                    WalletAbiFlowIntent.Retry -> retryFromError()
                    else -> Unit
                }
            }.onFailure { error ->
                showOverlayActionFailure(error)
            }
        }
    }

    private suspend fun handleApprove() {
        if (overlayActionMutex.isLocked) return
        overlayActionMutex.withLock {
            val isRequestApproval = isCurrentWalletAbiRequestApproval()
            requestApprovalInFlight = isRequestApproval
            markOverlayActionStarted()
            val outcome = walletAbiWalletConnectManager.approveCurrentOverlay(greenWallet.id)
            if (outcome != null) {
                handleOutcome(outcome)
            } else {
                requestApprovalInFlight = false
                if (isRequestApproval) {
                    postSideEffect(SideEffects.NavigateBack())
                } else {
                    synchronizeFromManager(currentManagerState())
                }
            }
        }
    }

    private suspend fun handleReject() {
        if (overlayActionMutex.isLocked) return
        overlayActionMutex.withLock {
            val isRequestApproval = isCurrentWalletAbiRequestApproval()
            requestApprovalInFlight = isRequestApproval
            markOverlayActionStarted()
            val outcome = walletAbiWalletConnectManager.rejectCurrentOverlay(greenWallet.id)
            if (outcome != null) {
                handleOutcome(outcome)
            } else {
                requestApprovalInFlight = false
                if (isRequestApproval) {
                    postSideEffect(SideEffects.NavigateBack())
                } else {
                    synchronizeFromManager(currentManagerState())
                }
            }
        }
    }

    private suspend fun handleCancel() {
        when (val state = mutableScreenState.value) {
            is WalletAbiWalletConnectScreenState.ConnectionApproval -> {
                handleReject()
            }

            is WalletAbiWalletConnectScreenState.WalletAbiFlow -> {
                if (state.state is WalletAbiFlowState.RequestLoaded) {
                    handleReject()
                }
            }

            is WalletAbiWalletConnectScreenState.Connected -> {
                if (overlayActionMutex.isLocked) return
                overlayActionMutex.withLock {
                    walletAbiWalletConnectManager.disconnectActiveSession(greenWallet.id)
                    synchronizeFromManager(currentManagerState())
                }
            }

            WalletAbiWalletConnectScreenState.Empty,
            WalletAbiWalletConnectScreenState.Pairing,
            is WalletAbiWalletConnectScreenState.AwaitingTransport,
            is WalletAbiWalletConnectScreenState.Preparing -> Unit
        }
    }

    private suspend fun dismissTerminal() {
        val shouldLeaveRoute = leaveRouteAfterTerminalDismiss || mutableScreenState.value.isTerminalFlow()
        leaveRouteAfterTerminalDismiss = false
        terminalOverride = null
        walletAbiWalletConnectManager.clearTerminal(greenWallet.id)
        if (currentManagerState().lastError != null) {
            walletAbiWalletConnectManager.clearError(greenWallet.id)
        }
        if (shouldLeaveRoute) {
            postSideEffect(SideEffects.NavigateBack())
            return
        }
        synchronizeFromManager(currentManagerState())
    }

    private suspend fun retryFromError() {
        leaveRouteAfterTerminalDismiss = false
        terminalOverride = null
        walletAbiWalletConnectManager.clearTerminal(greenWallet.id)
        if (currentManagerState().lastError != null) {
            walletAbiWalletConnectManager.clearError(greenWallet.id)
        }
        synchronizeFromManager(currentManagerState())
    }

    private suspend fun handleOutcome(outcome: WalletAbiWalletConnectActionOutcome) {
        val wasRequestApprovalInFlight = requestApprovalInFlight
        requestApprovalInFlight = false
        terminalOverride = when (outcome) {
            WalletAbiWalletConnectActionOutcome.SessionApproved -> null
            WalletAbiWalletConnectActionOutcome.SessionDisconnected -> null
            WalletAbiWalletConnectActionOutcome.SessionRejected ->
                WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected)

            is WalletAbiWalletConnectActionOutcome.RequestSucceeded ->
                outcome.toTerminalOverride()

            is WalletAbiWalletConnectActionOutcome.RequestRejected ->
                outcome.toTerminalOverride()
        }

        terminalOverride?.also { terminal ->
            leaveRouteAfterTerminalDismiss = when (outcome) {
                is WalletAbiWalletConnectActionOutcome.RequestRejected,
                is WalletAbiWalletConnectActionOutcome.RequestSucceeded,
                -> true

                WalletAbiWalletConnectActionOutcome.SessionApproved,
                WalletAbiWalletConnectActionOutcome.SessionDisconnected,
                WalletAbiWalletConnectActionOutcome.SessionRejected,
                -> false
            }
            showTerminal(terminal)
        } ?: if (wasRequestApprovalInFlight) {
            postSideEffect(SideEffects.NavigateBack())
        } else {
            synchronizeFromManager(currentManagerState())
        }
    }

    private fun markOverlayActionStarted() {
        when (val current = mutableScreenState.value) {
            is WalletAbiWalletConnectScreenState.ConnectionApproval -> {
                mutableScreenState.value = current.copy(
                    look = current.look.copy(awaitingTransport = true),
                )
            }

            is WalletAbiWalletConnectScreenState.WalletAbiFlow -> {
                val flowState = current.state
                if (flowState is WalletAbiFlowState.RequestLoaded) {
                    mutableScreenState.value = current.copy(
                        state = WalletAbiFlowState.Submitting(
                            requestContext = flowState.review.requestContext,
                            stage = WalletAbiSubmittingStage.PREPARING,
                        ),
                    )
                }
            }

            WalletAbiWalletConnectScreenState.Empty,
            WalletAbiWalletConnectScreenState.Pairing,
            is WalletAbiWalletConnectScreenState.AwaitingTransport,
            is WalletAbiWalletConnectScreenState.Connected,
            is WalletAbiWalletConnectScreenState.Preparing -> Unit
        }
    }

    private suspend fun synchronizeFromManager(state: WalletAbiWalletConnectState) {
        val overlay = state.uiState.currentOverlay
        if (overlay == null) {
            state.terminalOutcome?.toTerminalOverride()?.let { terminal ->
                terminalOverride = terminal
                leaveRouteAfterTerminalDismiss = true
                showTerminal(terminal)
                return
            }
        }

        when (overlay?.kind) {
            WalletAbiWalletConnectOverlayKind.CONNECTION_APPROVAL -> {
                pairingInFlight = false
                val proposal = overlay.proposal ?: return
                mutableScreenState.value = WalletAbiWalletConnectScreenState.ConnectionApproval(
                    WalletAbiWalletConnectConnectionLook(
                        proposal = proposal,
                        chainId = overlay.chainId,
                        methods = listOf(
                            proposal.requiredMethods,
                            proposal.optionalMethods,
                        ).flatten().distinct(),
                        awaitingTransport = overlay.awaitingTransport,
                        queuedOverlayCount = state.uiState.queuedOverlayCount.toInt(),
                    ),
                )
            }

            WalletAbiWalletConnectOverlayKind.TRANSACTION_APPROVAL -> {
                pairingInFlight = false
                val request = decodeWalletAbiRequest(overlay.requestJson)
                    ?: return showUnsupportedRequest("WalletConnect request payload is unavailable")
                val preview = decodeWalletAbiPreview(overlay.previewJson)
                    ?: return showUnsupportedRequest("WalletConnect preview payload is unavailable")
                val selectedAccount = selectWalletAbiAccount(request.network)
                    ?: return showUnsupportedRequest("WalletConnect requires an active Liquid singlesig account")
                val requestFamily = request.requestFamily()
                val review = request.toFlowReview(
                    walletId = greenWallet.id,
                    selectedAccount = selectedAccount,
                    requestFamily = requestFamily,
                )
                val look = buildWalletConnectReviewLook(
                    selectedAccount = selectedAccount,
                    request = request,
                    requestFamily = requestFamily,
                    preview = preview,
                )
                if (overlay.awaitingTransport) {
                    val session = state.uiState.activeSessions.firstOrNull { it.topic == overlay.sessionTopic }
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.AwaitingTransport(
                        WalletAbiWalletConnectAwaitingTransportLook(
                            requestId = request.requestId,
                            peerName = session?.peerName,
                            chainId = overlay.chainId,
                        ),
                    )
                } else {
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.WalletAbiFlow(
                        state = WalletAbiFlowState.RequestLoaded(review),
                        reviewLook = look,
                    )
                }
            }

            null -> {
                if (state.lastError != null) {
                    pairingInFlight = false
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.WalletAbiFlow(
                        state = WalletAbiFlowState.Error(
                            WalletAbiFlowError(
                                kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                                phase = WalletAbiFlowPhase.APPROVAL,
                                message = state.lastError ?: "WalletConnect failed",
                                retryable = true,
                            )
                        ),
                        reviewLook = null,
                    )
                    return
                }
                if (state.isPairing) {
                    pairingInFlight = false
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.Pairing
                    return
                }
                state.preparingRequest?.let { preparingRequest ->
                    pairingInFlight = false
                    val session = state.uiState.activeSessions.firstOrNull { it.topic == preparingRequest.topic }
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.Preparing(
                        WalletAbiWalletConnectPreparingLook(
                            requestId = preparingRequest.requestId,
                            peerName = session?.peerName,
                            chainId = session?.chainId ?: "",
                        ),
                    )
                    return
                }
                if (requestApprovalInFlight) {
                    return
                }
                if (pairingInFlight) {
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.Pairing
                    return
                }
                state.uiState.activeSessions.firstOrNull()?.also { sessionInfo ->
                    pairingInFlight = false
                    mutableScreenState.value = WalletAbiWalletConnectScreenState.Connected(
                        WalletAbiWalletConnectConnectedLook(
                            session = sessionInfo,
                            awaitingTransport = state.uiState.pendingActionCount > 0u,
                        ),
                    )
                    return
                }
                pairingInFlight = false
                mutableScreenState.value = WalletAbiWalletConnectScreenState.Empty
            }
        }
    }

    private fun showTerminal(terminal: WalletAbiFlowState) {
        mutableScreenState.value = WalletAbiWalletConnectScreenState.WalletAbiFlow(
            state = terminal,
            reviewLook = null,
        )
    }

    private fun shouldPreemptTerminal(
        state: WalletAbiWalletConnectState,
    ): Boolean {
        if (terminalOverride == null) {
            return false
        }

        return state.uiState.currentOverlay != null
    }

    private fun showUnsupportedRequest(message: String) {
        mutableScreenState.value = WalletAbiWalletConnectScreenState.WalletAbiFlow(
            state = WalletAbiFlowState.Error(
                WalletAbiFlowError(
                    kind = WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST,
                    phase = WalletAbiFlowPhase.LOADING,
                    message = message,
                    retryable = false,
                )
            ),
            reviewLook = null,
        )
    }

    private fun showOverlayActionFailure(error: Throwable) {
        requestApprovalInFlight = false
        mutableScreenState.value = WalletAbiWalletConnectScreenState.WalletAbiFlow(
            state = WalletAbiFlowState.Error(
                WalletAbiFlowError(
                    kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                    phase = WalletAbiFlowPhase.APPROVAL,
                    message = error.message ?: "WalletConnect action failed",
                    retryable = true,
                )
            ),
            reviewLook = null,
        )
    }

    private fun isCurrentWalletAbiRequestApproval(): Boolean {
        val state = mutableScreenState.value as? WalletAbiWalletConnectScreenState.WalletAbiFlow
            ?: return false
        return state.state is WalletAbiFlowState.RequestLoaded
    }

    private fun currentManagerState(): WalletAbiWalletConnectState {
        return walletAbiWalletConnectManager.state(greenWallet.id).value
    }

    private suspend fun buildWalletConnectReviewLook(
        selectedAccount: Account,
        request: WalletAbiTxCreateRequest,
        requestFamily: WalletAbiRequestFamily,
        preview: WalletAbiProviderRequestPreview,
    ): WalletAbiReviewLook {
        val accountAssetBalance = AccountAssetBalance.create(
            accountAsset = selectedAccount.accountAsset,
            session = session,
            denomination = denomination.value,
        )
        val outputs = preview.outputs.take(request.params.outputs.size).mapIndexed { index, output ->
            val requestedOutput = request.params.outputs.getOrNull(index)
            WalletAbiReviewOutputLook(
                address = requestedOutput?.summaryAddress() ?: "Wallet ABI output",
                amount = formatWalletAbiAmount(
                    amountSat = output.amountSat,
                    assetId = output.assetId,
                    account = selectedAccount,
                ),
                amountFiat = null,
                assetId = output.assetId,
                recipientScript = output.scriptPubkey,
            )
        }.ifEmpty {
            request.params.outputs.map { output ->
                WalletAbiReviewOutputLook(
                    address = output.summaryAddress(),
                    amount = formatWalletAbiAmount(
                        amountSat = output.amountSat,
                        assetId = output.explicitAssetIdOrNull(),
                        account = selectedAccount,
                    ),
                    amountFiat = null,
                    assetId = output.explicitAssetIdOrNull().orEmpty(),
                    recipientScript = output.scriptHexOrNull(),
                )
            }
        }
        val primaryOutput = outputs.firstOrNull() ?: WalletAbiReviewOutputLook(
            address = "Wallet ABI output",
            amount = "n/a",
            amountFiat = null,
            assetId = selectedAccount.network.policyAsset,
            recipientScript = null,
        )
        val primaryAssetId = primaryOutput.assetId.ifBlank { selectedAccount.network.policyAsset }
        val feeRate = request.params.feeRateSatKvb
            ?.toLong()
            ?.takeIf { it > 0L }
            ?.feeRateWithUnit()
        val warnings = buildList {
            addAll(preview.warnings)
            if (requestFamily == WalletAbiRequestFamily.REISSUANCE) {
                request.params.inputs.firstOrNull()
                    ?.walletFilterExactAssetIdOrNull()
                    ?.let { add("Reissuance consumes wallet token asset $it.") }
            }
        }.distinct()

        return WalletAbiReviewLook(
            accountAssetBalance = accountAssetBalance,
            outputs = outputs,
            recipientAddress = primaryOutput.address,
            amount = primaryOutput.amount,
            amountFiat = primaryOutput.amountFiat,
            assetName = if (primaryAssetId == selectedAccount.network.policyAsset) {
                selectedAccount.accountAsset.asset.name ?: primaryAssetId
            } else {
                primaryAssetId
            },
            assetTicker = selectedAccount.accountAsset.asset.ticker
                ?.takeIf { primaryAssetId == selectedAccount.network.policyAsset },
            assetId = primaryAssetId,
            networkName = selectedAccount.network.name.ifBlank { request.network.wireValue },
            networkWireValue = request.network.wireValue,
            method = WalletAbiMethod.PROCESS_REQUEST.wireValue,
            abiVersion = request.abiVersion,
            requestId = request.requestId,
            broadcast = request.broadcast,
            recipientScript = primaryOutput.recipientScript,
            transactionConfirmation = preview.toTransactionConfirmation(
                request = request,
                requestedOutputs = outputs,
                selectedAccount = selectedAccount,
                formatAmount = { amountSat, assetId ->
                    formatWalletAbiAmount(
                        amountSat = amountSat,
                        assetId = assetId,
                        account = selectedAccount,
                    )
                },
            )?.copy(feeRate = feeRate),
            requestFamily = requestFamily,
            resolutionState = WalletAbiResolutionState.READY,
            statusMessage = "Exact WalletConnect review is ready. Approval will respond to the paired app.",
            warnings = warnings,
            assetImpacts = preview.assetDeltas.map { delta ->
                WalletAbiReviewAssetImpactLook(
                    assetId = delta.assetId,
                    walletDelta = formatWalletAbiSignedAmount(
                        amountSat = delta.walletDeltaSat,
                        assetId = delta.assetId,
                        account = selectedAccount,
                    ),
                )
            },
            canResolve = false,
            canApprove = true,
        )
    }

    private suspend fun formatWalletAbiAmount(
        amountSat: Long,
        assetId: String?,
        account: Account,
    ): String {
        val normalizedAssetId = assetId?.takeIf { it.isNotBlank() }
        return amountSat.toAmountLook(
            session = session,
            assetId = normalizedAssetId,
            denomination = normalizedAssetId
                ?.takeIf { it == account.network.policyAsset }
                ?.let { denomination.value },
        ) ?: buildString {
            append(amountSat)
            append(" sat")
            normalizedAssetId?.let {
                append(" · ")
                append(it.take(8))
            }
        }
    }

    private suspend fun formatWalletAbiSignedAmount(
        amountSat: Long,
        assetId: String,
        account: Account,
    ): String {
        val formatted = formatWalletAbiAmount(
            amountSat = amountSat.absoluteValue,
            assetId = assetId,
            account = account,
        )
        return when {
            amountSat > 0L -> "+$formatted"
            amountSat < 0L -> "-$formatted"
            else -> formatted
        }
    }

    private fun selectWalletAbiAccount(requestNetwork: WalletAbiNetwork): Account? {
        return session.activeAccount.value
            ?.takeIf { it.isSupportedWalletAbiAccount() && it.network.matchesWalletAbiRequestNetwork(requestNetwork) }
            ?: session.accounts.value.firstOrNull { account ->
                account.isSupportedWalletAbiAccount() &&
                    account.network.matchesWalletAbiRequestNetwork(requestNetwork)
            }
    }
}

private fun decodeWalletAbiRequest(requestJson: String?): WalletAbiTxCreateRequest? {
    val payload = requestJson ?: return null
    return runCatching {
        DefaultJson.decodeFromString(WalletAbiTxCreateRequest.serializer(), payload)
    }.recoverCatching {
        DefaultJson.decodeFromString(
            WalletAbiTxCreateRequest.serializer(),
            normalizeWalletAbiNetworkAliases(payload),
        )
    }.getOrNull()
}

private fun decodeWalletAbiPreview(previewJson: String?): WalletAbiProviderRequestPreview? {
    val payload = previewJson ?: return null
    return runCatching {
        DefaultJson.decodeFromString(WalletAbiProviderRequestPreview.serializer(), payload)
    }.getOrNull()
}

private fun normalizeWalletAbiNetworkAliases(payload: String): String {
    val requestObject = runCatching {
        DefaultJson.parseToJsonElement(payload).jsonObject
    }.getOrNull() ?: return payload

    val normalizedNetwork = when (val network = requestObject["network"]) {
        is JsonPrimitive -> when (network.contentOrNull) {
            "Liquid" -> JsonPrimitive("liquid")
            "LiquidTestnet" -> JsonPrimitive("testnet-liquid")
            "ElementsRegtest" -> JsonPrimitive("localtest-liquid")
            else -> null
        }

        is JsonObject -> {
            if ("ElementsRegtest" in network) {
                JsonPrimitive("localtest-liquid")
            } else {
                null
            }
        }

        else -> null
    } ?: return payload

    return buildJsonObject {
        requestObject.forEach { (key, value: JsonElement) ->
            put(key, if (key == "network") normalizedNetwork else value)
        }
    }.toString()
}

private fun String.toWalletAbiTerminalState(): WalletAbiFlowState {
    return runCatching {
        DefaultJson.decodeFromString(
            WalletAbiProviderProcessResponse.serializer(),
            normalizeWalletAbiNetworkAliases(this),
        )
    }.fold(
        onSuccess = { response ->
            when (response.status) {
                WalletAbiProviderStatus.OK -> WalletAbiFlowState.Success(
                    WalletAbiSuccessResult(
                        requestId = response.requestId,
                        txHash = response.transaction?.txid,
                        responseId = response.requestId,
                    )
                )

                WalletAbiProviderStatus.ERROR -> {
                    val errorMessage = response.error?.message ?: "WalletConnect request failed"
                    if (errorMessage.isUserRejectedMessage()) {
                        WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected)
                    } else {
                        WalletAbiFlowState.Error(
                            WalletAbiFlowError(
                                kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                                phase = WalletAbiFlowPhase.SUBMISSION,
                                message = errorMessage,
                                retryable = false,
                            )
                        )
                    }
                }
            }
        },
        onFailure = { error ->
            WalletAbiFlowState.Error(
                WalletAbiFlowError(
                    kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                    phase = WalletAbiFlowPhase.SUBMISSION,
                    message = error.message ?: "WalletConnect returned an invalid response",
                    retryable = false,
                )
            )
        },
    )
}

private fun WalletAbiWalletConnectActionOutcome.toTerminalOverride(): WalletAbiFlowState? {
    return when (this) {
        is WalletAbiWalletConnectActionOutcome.RequestSucceeded ->
            resultJson.takeIf { method == WALLET_ABI_PROCESS_REQUEST_METHOD }?.toWalletAbiTerminalState()
        is WalletAbiWalletConnectActionOutcome.RequestRejected ->
            message.takeIf { method == WALLET_ABI_PROCESS_REQUEST_METHOD }?.let { errorMessage ->
                if (errorMessage.isUserRejectedMessage()) {
                    WalletAbiFlowState.Cancelled(WalletAbiCancelledReason.UserRejected)
                } else {
                    WalletAbiFlowState.Error(
                        WalletAbiFlowError(
                            kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                            phase = WalletAbiFlowPhase.SUBMISSION,
                            message = errorMessage,
                            retryable = false,
                        )
                    )
                }
            }

        WalletAbiWalletConnectActionOutcome.SessionApproved,
        WalletAbiWalletConnectActionOutcome.SessionDisconnected,
        WalletAbiWalletConnectActionOutcome.SessionRejected,
        -> null
    }
}

private fun WalletAbiWalletConnectScreenState.isTerminalFlow(): Boolean {
    val flowState = (this as? WalletAbiWalletConnectScreenState.WalletAbiFlow)?.state
        ?: return false
    return flowState is WalletAbiFlowState.Success ||
        flowState is WalletAbiFlowState.Cancelled ||
        flowState is WalletAbiFlowState.Error
}

private fun String.isUserRejectedMessage(): Boolean {
    return trim().equals(USER_REJECTED_MESSAGE, ignoreCase = true) ||
        contains("rejected by user", ignoreCase = true)
}

private fun WalletAbiTxCreateRequest.toFlowReview(
    walletId: String,
    selectedAccount: Account,
    requestFamily: WalletAbiRequestFamily,
): WalletAbiFlowReview {
    return WalletAbiFlowReview(
        requestContext = WalletAbiStartRequestContext(
            requestId = requestId,
            walletId = walletId,
        ),
        method = WalletAbiMethod.PROCESS_REQUEST.wireValue,
        title = when (requestFamily) {
            WalletAbiRequestFamily.PAYMENT -> "Wallet ABI transfer"
            WalletAbiRequestFamily.SPLIT -> "Wallet ABI split"
            WalletAbiRequestFamily.ISSUANCE -> "Wallet ABI issuance"
            WalletAbiRequestFamily.REISSUANCE -> "Wallet ABI reissuance"
        },
        message = "Review the WalletConnect request before approving it in Green.",
        accounts = listOf(
            WalletAbiAccountOption(
                accountId = selectedAccount.id,
                name = selectedAccount.name,
            )
        ),
        selectedAccountId = selectedAccount.id,
        approvalTarget = WalletAbiApprovalTarget.Software,
        executionDetails = WalletAbiExecutionDetails(
            destinationAddress = params.outputs.firstOrNull()?.summaryAddress() ?: "Wallet ABI output",
            amountSat = params.outputs.sumOf { it.amountSat },
            assetId = params.outputs.firstOrNull()?.explicitAssetIdOrNull()
                ?: selectedAccount.network.policyAsset,
            network = network.wireValue,
            feeRate = params.feeRateSatKvb?.toLong(),
            requestFamily = requestFamily,
            resolutionState = WalletAbiResolutionState.READY,
            outputCount = params.outputs.size,
        ),
        parsedRequest = com.blockstream.domain.walletabi.request.WalletAbiParsedRequest.TxCreate(this),
    )
}

private suspend fun WalletAbiProviderRequestPreview.toTransactionConfirmation(
    request: WalletAbiTxCreateRequest,
    requestedOutputs: List<WalletAbiReviewOutputLook>,
    selectedAccount: Account,
    formatAmount: suspend (Long, String?) -> String,
): com.blockstream.data.transaction.TransactionConfirmation? {
    val feeOutput = outputs.drop(request.params.outputs.size)
        .firstOrNull { it.kind == WalletAbiProviderPreviewOutputKind.FEE }
    val fee = feeOutput?.let { output ->
        formatAmount(output.amountSat, output.assetId)
    }
    val totalSpentSat = assetDeltas
        .firstOrNull { delta ->
            delta.assetId == selectedAccount.network.policyAsset && delta.walletDeltaSat < 0L
        }
        ?.walletDeltaSat
        ?.absoluteValue
    val total = totalSpentSat?.let { satoshi ->
        formatAmount(satoshi, selectedAccount.network.policyAsset)
    }

    return com.blockstream.data.transaction.TransactionConfirmation(
        utxos = requestedOutputs.mapIndexed { index, output ->
            com.blockstream.data.gdk.data.UtxoView(
                address = output.address,
                assetId = output.assetId,
                satoshi = request.params.outputs.getOrNull(index)?.amountSat ?: 0L,
                amount = output.amount,
                amountExchange = output.amountFiat,
            )
        },
        fee = fee,
        feeAssetId = feeOutput?.assetId,
        total = total,
    )
}

private fun WalletAbiTxCreateRequest.requestFamily(): WalletAbiRequestFamily {
    val outputAssetTypes = params.outputs.mapNotNull { it.assetTypeOrNull() }.toSet()
    return when {
        "re_issuance_asset" in outputAssetTypes ||
            params.inputs.any { it.issuanceKindOrNull() == "reissue" } -> WalletAbiRequestFamily.REISSUANCE

        "new_issuance_asset" in outputAssetTypes ||
            "new_issuance_token" in outputAssetTypes ||
            params.inputs.any { it.issuanceKindOrNull() == "new" } -> WalletAbiRequestFamily.ISSUANCE

        params.outputs.size > 1 -> WalletAbiRequestFamily.SPLIT
        else -> WalletAbiRequestFamily.PAYMENT
    }
}

private fun Account.isSupportedWalletAbiAccount(): Boolean {
    return isLiquid && !isLightning && !isMultisig
}

private fun Network.matchesWalletAbiRequestNetwork(requestNetwork: WalletAbiNetwork): Boolean {
    return when (requestNetwork) {
        WalletAbiNetwork.LIQUID -> isLiquidMainnet
        WalletAbiNetwork.TESTNET_LIQUID -> isLiquidTestnet && !isDevelopment
        WalletAbiNetwork.LOCALTEST_LIQUID -> isLiquid && isDevelopment
    }
}

private fun WalletAbiInput.issuanceKindOrNull(): String? {
    val issuanceObject = issuance as? JsonObject ?: return null
    return issuanceObject["kind"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
}

private fun WalletAbiInput.walletFilterExactAssetIdOrNull(): String? {
    val utxoSourceObject = utxoSource as? JsonObject ?: return null
    val walletObject = (utxoSourceObject["wallet"] as? JsonObject) ?: utxoSourceObject
    val filterObject = walletObject["filter"] as? JsonObject ?: return null
    val assetFilter = filterObject["asset"] ?: return null
    return when (assetFilter) {
        is JsonObject -> {
            val exactObject = assetFilter["exact"] as? JsonObject ?: assetFilter
            exactObject["asset_id"]?.jsonPrimitive?.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

        else -> null
    }
}

private fun WalletAbiOutput.assetTypeOrNull(): String? {
    val assetObject = asset as? JsonObject ?: return null
    return assetObject["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
}

private fun WalletAbiOutput.explicitAssetIdOrNull(): String? {
    val assetObject = asset as? JsonObject ?: return null
    return assetObject["asset_id"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
}

private fun WalletAbiOutput.scriptHexOrNull(): String? {
    val lockObject = lock as? JsonObject ?: return null
    if (lockObject["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase() != "script") {
        return null
    }
    return lockObject["script"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
}

private fun WalletAbiOutput.summaryAddress(): String {
    val lockObject = lock as? JsonObject ?: return "Wallet ABI output"
    return when (lockObject["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()) {
        "address" -> sequenceOf(
            lockObject["address"]?.jsonPrimitive?.contentOrNull,
            (lockObject["recipient"] as? JsonObject)?.get("confidential_address")?.jsonPrimitive?.contentOrNull,
            (lockObject["recipient"] as? JsonObject)?.get("address")?.jsonPrimitive?.contentOrNull,
            (lockObject["recipient"] as? JsonObject)?.get("unconfidential_address")?.jsonPrimitive?.contentOrNull,
        ).firstOrNull { !it.isNullOrBlank() } ?: "Wallet ABI output"

        "wallet" -> when (assetTypeOrNull()) {
            "new_issuance_asset" -> "Issued asset to wallet"
            "new_issuance_token" -> "Reissuance token to wallet"
            "re_issuance_asset" -> "Reissued asset to wallet"
            else -> "Wallet-controlled output"
        }

        "script" -> scriptHexOrNull()?.let { "Script ${it.take(24)}" } ?: "Script output"
        "finalizer" -> "Finalized output"
        else -> "Wallet ABI output"
    }
}

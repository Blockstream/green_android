package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.WalletAbiFlowLaunchMode
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.walletabi.request.WalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContext
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContextException
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContextResolver
import com.blockstream.domain.walletabi.provider.WalletAbiExecutionContextResolving
import com.blockstream.domain.walletabi.provider.WalletAbiProviderPreviewOutputKind
import com.blockstream.domain.walletabi.provider.WalletAbiProviderProcessResponse
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRequestPreview
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunner
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunning
import com.blockstream.domain.walletabi.provider.WalletAbiProviderStatus
import com.blockstream.domain.walletabi.provider.WalletAbiSignerBackend
import com.blockstream.domain.walletabi.provider.preview
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiReviewPreviewer
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlan
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiPreparedExecution
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionValidationError
import com.blockstream.domain.walletabi.execution.WalletAbiReviewPreviewer
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionValidationException
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiCancelledReason
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionEvent
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionDetails
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiFlowError
import com.blockstream.domain.walletabi.flow.WalletAbiFlowErrorKind
import com.blockstream.domain.walletabi.flow.WalletAbiFlowPhase
import com.blockstream.domain.walletabi.flow.WalletAbiJadeEvent
import com.blockstream.domain.walletabi.flow.WalletAbiRequestFamily
import com.blockstream.domain.walletabi.flow.WalletAbiResolutionState
import com.blockstream.domain.walletabi.flow.WalletAbiResumePhase
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.DefaultWalletAbiRequestParser
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiMethod
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedEnvelope
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRequestParseResult
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import com.blockstream.domain.walletabi.request.WalletAbiRequestValidationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import lwk.WalletAbiRequestSession
import kotlin.math.absoluteValue

class WalletAbiFlowRouteViewModel(
    greenWallet: GreenWallet,
    private val launchMode: WalletAbiFlowLaunchMode = WalletAbiFlowLaunchMode.Demo,
    private val store: WalletAbiFlowStore,
    private val snapshotRepository: WalletAbiFlowSnapshotRepository,
    private val walletSession: GdkSession,
    private val requestSource: WalletAbiDemoRequestSource = DefaultWalletAbiDemoRequestSource(),
    private val executionPlanner: WalletAbiExecutionPlanner = DefaultWalletAbiExecutionPlanner(),
    private val executionRunner: WalletAbiExecutionRunner = DefaultWalletAbiExecutionRunner(),
    private val reviewPreviewer: WalletAbiReviewPreviewer = DefaultWalletAbiReviewPreviewer(),
    private val executionContextResolver: WalletAbiExecutionContextResolving = WalletAbiExecutionContextResolver(),
    private val providerRunner: WalletAbiProviderRunning = object : WalletAbiProviderRunning {
        override suspend fun run(
            context: WalletAbiExecutionContext,
            request: WalletAbiTxCreateRequest,
        ) = error("Wallet ABI provider runner is not configured")
    },
    private val loadingTimeoutMillis: Long = 15_000L,
    private val approvalTimeoutMillis: Long = 120_000L,
    private val submissionTimeoutMillis: Long = 60_000L
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    private val requestParser = DefaultWalletAbiRequestParser()
    private val mutableReviewLook = MutableStateFlow<WalletAbiReviewLook?>(null)
    private var loadingJob: Job? = null
    private var approvalJob: Job? = null
    private var reviewRefreshJob: Job? = null
    private var submissionJob: Job? = null
    private var activeEnvelope: WalletAbiParsedEnvelope? = null
    private var activeReview: WalletAbiFlowReview? = null
    private var activePreparedExecution: WalletAbiPreparedExecution? = null
    private var activeProviderPreparedExecution: WalletAbiProviderPreparedExecution? = null
    private var eligibleAccounts: List<Account> = emptyList()

    override fun screenName(): String = "WalletAbiFlow"
    override val isLoginRequired: Boolean = false

    val state: StateFlow<WalletAbiFlowState> = store.state
    val reviewLook: StateFlow<WalletAbiReviewLook?> = mutableReviewLook.asStateFlow()

    init {
        _navData.value = NavData(
            title = "Wallet ABI",
            subtitle = greenWallet.name
        )
        store.outputs.onEach { output ->
            handleOutput(output = output, greenWallet = greenWallet)
        }.launchIn(viewModelScope)
        bootstrapFlow(greenWallet = greenWallet)
        bootstrap()
    }

    private suspend fun handleOutput(output: WalletAbiFlowOutput, greenWallet: GreenWallet) {
        when (output) {
            is WalletAbiFlowOutput.LoadRequest -> {
                launchLoadingRequest(output.requestContext)
            }

            is WalletAbiFlowOutput.PersistSnapshot -> {
                val snapshot = output.snapshot
                if (snapshot == null) {
                    snapshotRepository.clear(greenWallet.id)
                } else {
                    snapshotRepository.save(greenWallet.id, snapshot)
                }
            }

            is WalletAbiFlowOutput.CancelActiveWork -> when (output.phase) {
                WalletAbiFlowPhase.LOADING -> {
                    loadingJob?.cancel()
                    store.dispatch(
                        WalletAbiFlowIntent.OnExecutionEvent(
                            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled)
                        )
                    )
                }
                WalletAbiFlowPhase.APPROVAL -> {
                    approvalJob?.cancel()
                    store.dispatch(
                        WalletAbiFlowIntent.OnExecutionEvent(
                            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled)
                        )
                    )
                }
                WalletAbiFlowPhase.SUBMISSION -> {
                    submissionJob?.cancel()
                    store.dispatch(
                        WalletAbiFlowIntent.OnExecutionEvent(
                            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled)
                        )
                    )
                }
            }
            is WalletAbiFlowOutput.StartResolution -> {
                launchReviewRefresh(
                    requestContext = output.command.requestContext,
                    selectedAccountId = output.command.selectedAccountId,
                    resolveExact = true,
                )
            }

            is WalletAbiFlowOutput.StartSubmission -> {
                approvalJob?.cancel()
                val providerPreparedExecution = activeProviderPreparedExecution
                    ?.takeIf { prepared ->
                        output.command.selectedAccountId == null ||
                            prepared.context.primaryAccount.id == output.command.selectedAccountId
                    }
                if (providerPreparedExecution != null) {
                    launchProviderSubmission(providerPreparedExecution)
                    return
                }
                val preparedExecution = activePreparedExecution
                    ?.takeIf { prepared ->
                        output.command.selectedAccountId == null ||
                            prepared.plan.selectedAccount.id == output.command.selectedAccountId
                    }
                    ?: return dispatchExecutionFailure(
                        WalletAbiFlowError(
                            kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                            phase = WalletAbiFlowPhase.SUBMISSION,
                            message = "Wallet ABI review is not ready",
                            retryable = true
                        )
                    )

                launchSubmission(preparedExecution)
            }

            is WalletAbiFlowOutput.Complete -> Unit
            is WalletAbiFlowOutput.StartApproval -> {
                launchApprovalTimeout()
                launchJadeApproval()
            }
        }
    }

    private fun launchJadeApproval() {
        viewModelScope.launch {
            try {
                if (walletSession.device?.isConnected != true) {
                    store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Disconnected))
                    return@launch
                }
                store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Connected))
                store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.UnlockConfirmed))
                store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.ReviewConfirmed))
                store.dispatch(WalletAbiFlowIntent.OnJadeEvent(WalletAbiJadeEvent.Signed))
            } catch (_: CancellationException) {
                Unit
            } catch (throwable: Throwable) {
                store.dispatch(
                    WalletAbiFlowIntent.OnJadeEvent(
                        WalletAbiJadeEvent.Failed(
                            throwable.toWalletAbiFlowError(
                                phase = WalletAbiFlowPhase.APPROVAL,
                                sessionConnected = walletSession.isConnected,
                            )
                        )
                    )
                )
            }
        }
    }

    private fun launchLoadingRequest(requestContext: WalletAbiStartRequestContext) {
        loadingJob?.cancel()
        reviewRefreshJob?.cancel()
        loadingJob = viewModelScope.launch {
            try {
                withTimeout(loadingTimeoutMillis) {
                    clearPreparedReview()
                    when (val parseResult = requestParser.parse(requestSource.loadRequestEnvelope(requestContext.requestId))) {
                        is WalletAbiRequestParseResult.Failure -> {
                            dispatchExecutionFailure(parseResult.error.toFlowError())
                        }

                        is WalletAbiRequestParseResult.Success -> {
                            activeEnvelope = parseResult.envelope
                            loadRequestReview(requestContext = requestContext)
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                dispatchExecutionFailure(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.TIMEOUT,
                        phase = WalletAbiFlowPhase.LOADING,
                        message = "Wallet ABI request timed out",
                        retryable = true
                    )
                )
            } catch (_: CancellationException) {
                Unit
            } finally {
                if (loadingJob == kotlinx.coroutines.currentCoroutineContext()[Job]) {
                    loadingJob = null
                }
            }
        }
    }

    private fun launchSubmission(preparedExecution: WalletAbiPreparedExecution) {
        submissionJob?.cancel()
        submissionJob = viewModelScope.launch {
            try {
                store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted))

                val preparedBroadcast = try {
                    withTimeout(submissionTimeoutMillis) {
                        executionRunner.prepare(
                            session = walletSession,
                            preparedExecution = preparedExecution
                        )
                    }
                } catch (_: TimeoutCancellationException) {
                    dispatchExecutionFailure(
                        WalletAbiFlowError(
                            kind = WalletAbiFlowErrorKind.TIMEOUT,
                            phase = WalletAbiFlowPhase.SUBMISSION,
                            message = "Wallet ABI submission timed out",
                            retryable = true
                        )
                    )
                    return@launch
                }
                currentCoroutineContext().ensureActive()

                store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted))

                val result = try {
                    withTimeout(submissionTimeoutMillis) {
                        executionRunner.broadcast(
                            session = walletSession,
                            preparedBroadcast = preparedBroadcast,
                            twoFactorResolver = this@WalletAbiFlowRouteViewModel
                        )
                    }
                } catch (_: TimeoutCancellationException) {
                    dispatchExecutionFailure(
                        WalletAbiFlowError(
                            kind = WalletAbiFlowErrorKind.PARTIAL_COMPLETION,
                            phase = WalletAbiFlowPhase.SUBMISSION,
                            message = "Transaction status may already have changed. Check your wallet activity before retrying.",
                            retryable = false
                        )
                    )
                    return@launch
                }
                store.dispatch(
                    WalletAbiFlowIntent.OnExecutionEvent(
                        WalletAbiExecutionEvent.RemoteResponseSent(
                            result = WalletAbiSuccessResult(
                                requestId = preparedExecution.plan.request.request.requestId,
                                txHash = result.txHash
                            )
                        )
                    )
                )
            } catch (_: CancellationException) {
                Unit
            } catch (throwable: Throwable) {
                dispatchExecutionFailure(
                    throwable.toWalletAbiFlowError(
                        phase = WalletAbiFlowPhase.SUBMISSION,
                        sessionConnected = walletSession.isConnected
                    )
                )
            } finally {
                if (submissionJob == currentCoroutineContext()[Job]) {
                    submissionJob = null
                }
            }
        }
    }

    private fun launchProviderSubmission(preparedExecution: WalletAbiProviderPreparedExecution) {
        submissionJob?.cancel()
        submissionJob = viewModelScope.launch {
            try {
                store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted))
                currentCoroutineContext().ensureActive()

                val providerResult = try {
                    withTimeout(submissionTimeoutMillis) {
                        providerRunner.process(
                            context = preparedExecution.context,
                            request = preparedExecution.request,
                            requestSession = preparedExecution.requestSession,
                        )
                    }
                } catch (_: TimeoutCancellationException) {
                    dispatchExecutionFailure(
                        WalletAbiFlowError(
                            kind = WalletAbiFlowErrorKind.TIMEOUT,
                            phase = WalletAbiFlowPhase.SUBMISSION,
                            message = "Wallet ABI provider signing timed out",
                            retryable = true,
                        )
                    )
                    return@launch
                }
                val signedTransaction = providerResult.response.transaction
                    ?: error("Wallet ABI provider response is missing tx_hex")

                store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted))

                val result = try {
                    withTimeout(submissionTimeoutMillis) {
                        walletSession.broadcastTransaction(
                            network = preparedExecution.context.primaryAccount.network,
                            broadcastTransaction = BroadcastTransactionParams(
                                transaction = signedTransaction.txHex
                            )
                        )
                    }
                } catch (_: TimeoutCancellationException) {
                    dispatchExecutionFailure(
                        WalletAbiFlowError(
                            kind = WalletAbiFlowErrorKind.PARTIAL_COMPLETION,
                            phase = WalletAbiFlowPhase.SUBMISSION,
                            message = "Transaction status may already have changed. Check your wallet activity before retrying.",
                            retryable = false,
                        )
                    )
                    return@launch
                }

                store.dispatch(
                    WalletAbiFlowIntent.OnExecutionEvent(
                        WalletAbiExecutionEvent.RemoteResponseSent(
                            result = WalletAbiSuccessResult(
                                requestId = preparedExecution.request.requestId,
                                txHash = result.txHash?.takeIf { it.isNotBlank() }
                                    ?: signedTransaction.txid,
                            )
                        )
                    )
                )
            } catch (_: CancellationException) {
                Unit
            } catch (throwable: Throwable) {
                dispatchExecutionFailure(
                    throwable.toWalletAbiFlowError(
                        phase = WalletAbiFlowPhase.SUBMISSION,
                        sessionConnected = walletSession.isConnected
                    )
                )
            } finally {
                if (submissionJob == currentCoroutineContext()[Job]) {
                    submissionJob = null
                }
            }
        }
    }

    private fun launchApprovalTimeout() {
        approvalJob?.cancel()
        approvalJob = viewModelScope.launch {
            try {
                withTimeout(approvalTimeoutMillis) {
                    awaitCancellation()
                }
            } catch (_: TimeoutCancellationException) {
                dispatchExecutionFailure(
                    WalletAbiFlowError(
                        kind = WalletAbiFlowErrorKind.TIMEOUT,
                        phase = WalletAbiFlowPhase.APPROVAL,
                        message = "Wallet ABI approval timed out",
                        retryable = true
                    )
                )
            } catch (_: CancellationException) {
                Unit
            } finally {
                if (approvalJob == currentCoroutineContext()[Job]) {
                    approvalJob = null
                }
            }
        }
    }

    private fun launchReviewRefresh(
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?,
        resolveExact: Boolean,
    ) {
        reviewRefreshJob?.cancel()
        reviewRefreshJob = viewModelScope.launch {
            try {
                refreshPreparedReview(
                    requestContext = requestContext,
                    selectedAccountId = selectedAccountId,
                    resolveExact = resolveExact,
                )
            } catch (_: CancellationException) {
                Unit
            } finally {
                if (reviewRefreshJob == kotlinx.coroutines.currentCoroutineContext()[Job]) {
                    reviewRefreshJob = null
                }
            }
        }
    }

    private suspend fun loadRequestReview(requestContext: WalletAbiStartRequestContext) {
        val envelope = activeEnvelope ?: return dispatchExecutionFailure(
            WalletAbiFlowError(
                kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                phase = WalletAbiFlowPhase.LOADING,
                message = "Wallet ABI request is not loaded",
                retryable = true
            )
        )
        val preparedReview = try {
            buildPreparedReview(
                envelope = envelope,
                requestContext = requestContext,
                selectedAccountId = null,
                resolveExact = false,
            )
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            dispatchExecutionFailure(
                throwable.toWalletAbiFlowError(
                    phase = WalletAbiFlowPhase.LOADING,
                    sessionConnected = walletSession.isConnected
                )
            )
            return
        }

        activeReview = preparedReview.review
        activePreparedExecution = preparedReview.preparedExecution
        activeProviderPreparedExecution = preparedReview.providerPreparedExecution
        mutableReviewLook.value = preparedReview.reviewLook
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.RequestLoaded(
                    review = preparedReview.review
                )
            )
        )
    }

    private suspend fun refreshPreparedReview(
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?,
        resolveExact: Boolean,
    ) {
        val envelope = activeEnvelope ?: return dispatchExecutionFailure(
            WalletAbiFlowError(
                kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                phase = WalletAbiFlowPhase.LOADING,
                message = "Wallet ABI request is not loaded",
                retryable = true
            )
        )
        markReviewRefreshing(selectedAccountId)
        val preparedReview = try {
            buildPreparedReview(
                envelope = envelope,
                requestContext = requestContext,
                selectedAccountId = selectedAccountId,
                resolveExact = resolveExact,
            )
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            dispatchExecutionFailure(
                throwable.toWalletAbiFlowError(
                    phase = WalletAbiFlowPhase.LOADING,
                    sessionConnected = walletSession.isConnected
                )
            )
            return
        }

        activeReview = preparedReview.review
        activePreparedExecution = preparedReview.preparedExecution
        activeProviderPreparedExecution = preparedReview.providerPreparedExecution
        mutableReviewLook.value = preparedReview.reviewLook
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Resolved(
                    review = preparedReview.review
                )
            )
        )
    }

    private suspend fun dispatchExecutionFailure(error: WalletAbiFlowError) {
        clearPreparedReview()
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    error
                )
            )
        )
    }

    fun dispatch(intent: WalletAbiFlowIntent) {
        viewModelScope.launch {
            val wasTerminal = store.state.value is WalletAbiFlowState.Success ||
                store.state.value is WalletAbiFlowState.Cancelled ||
                store.state.value is WalletAbiFlowState.Error
            store.dispatch(intent)
            if (intent is WalletAbiFlowIntent.OnJadeEvent) {
                when (intent.event) {
                    WalletAbiJadeEvent.Cancelled,
                    WalletAbiJadeEvent.Disconnected,
                    is WalletAbiJadeEvent.Failed,
                    WalletAbiJadeEvent.Signed -> approvalJob?.cancel()
                    else -> Unit
                }
            }
            if (intent is WalletAbiFlowIntent.SelectAccount) {
                val currentReview = activeReview ?: return@launch
                val resolveExact = currentReview.executionDetails?.resolutionState == WalletAbiResolutionState.READY ||
                    currentReview.executionDetails?.resolutionState == WalletAbiResolutionState.NOT_REQUIRED
                launchReviewRefresh(
                    requestContext = currentReview.requestContext,
                    selectedAccountId = intent.accountId,
                    resolveExact = resolveExact,
                )
            }
            if (intent == WalletAbiFlowIntent.DismissTerminal &&
                wasTerminal &&
                store.state.value == WalletAbiFlowState.Idle
            ) {
                postSideEffect(SideEffects.NavigateBack())
            }
        }
    }

    private fun bootstrapFlow(greenWallet: GreenWallet) {
        viewModelScope.launch {
            when (launchMode) {
                WalletAbiFlowLaunchMode.Demo -> {
                    store.dispatch(
                        WalletAbiFlowIntent.Start(
                            requestContext = WalletAbiStartRequestContext(
                                requestId = DEMO_REQUEST_ID,
                                walletId = greenWallet.id
                            )
                        )
                    )
                }

                WalletAbiFlowLaunchMode.Resume -> {
                    restoreFlow(greenWallet = greenWallet)
                }
            }
        }
    }

    private suspend fun restoreFlow(greenWallet: GreenWallet) {
        val snapshot = snapshotRepository.load(greenWallet.id)
        if (snapshot == null) {
            clearPreparedReview()
            postSideEffect(SideEffects.NavigateBack())
            return
        }

        when (snapshot.phase) {
            WalletAbiResumePhase.REQUEST_LOADED,
            WalletAbiResumePhase.AWAITING_APPROVAL -> {
                val parsedRequest = snapshot.review.parsedRequest
                val method = snapshot.review.method.toWalletAbiMethodOrNull()
                if (parsedRequest == null || method == null) {
                    snapshotRepository.clear(greenWallet.id)
                    clearPreparedReview()
                    postSideEffect(SideEffects.NavigateBack())
                    return
                }

                val preparedReview = try {
                    val restoredEnvelope = WalletAbiParsedEnvelope(
                        id = null,
                        method = method,
                        request = parsedRequest
                    )
                    activeEnvelope = restoredEnvelope
                    val resolveExact = snapshot.review.executionDetails?.resolutionState == WalletAbiResolutionState.READY
                    buildPreparedReview(
                        envelope = restoredEnvelope,
                        requestContext = snapshot.review.requestContext,
                        selectedAccountId = snapshot.review.selectedAccountId,
                        resolveExact = resolveExact,
                    )
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    snapshotRepository.clear(greenWallet.id)
                    dispatchExecutionFailure(
                        throwable.toWalletAbiFlowError(
                            phase = WalletAbiFlowPhase.LOADING,
                            sessionConnected = walletSession.isConnected
                        ).copy(retryable = false)
                    )
                    return
                }

                activeReview = preparedReview.review
                activePreparedExecution = preparedReview.preparedExecution
                activeProviderPreparedExecution = preparedReview.providerPreparedExecution
                mutableReviewLook.value = preparedReview.reviewLook
                store.dispatch(
                    WalletAbiFlowIntent.Restore(
                        snapshot.copy(
                            review = preparedReview.review
                        )
                    )
                )
            }

            WalletAbiResumePhase.SUBMITTING -> {
                clearPreparedReview()
                store.dispatch(
                    WalletAbiFlowIntent.Restore(snapshot)
                )
            }
        }
    }

    private suspend fun buildPreparedReview(
        envelope: WalletAbiParsedEnvelope,
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?,
        resolveExact: Boolean,
    ): PreparedWalletAbiReview {
        envelope.providerRequestFamilyOrNull()?.let { requestFamily ->
            return if (resolveExact) {
                buildResolvedProviderReview(
                    envelope = envelope,
                    requestContext = requestContext,
                    selectedAccountId = selectedAccountId,
                    requestFamily = requestFamily,
                )
            } else {
                buildUnresolvedProviderReview(
                    envelope = envelope,
                    requestContext = requestContext,
                    selectedAccountId = selectedAccountId,
                    requestFamily = requestFamily,
                )
            }
        }
        val executionPlan = executionPlanner.plan(
            session = walletSession,
            request = envelope.request,
            selectedAccountId = selectedAccountId
        )
        eligibleAccounts = executionPlan.accounts
        val preparedExecution = reviewPreviewer.prepare(
            session = walletSession,
            plan = executionPlan,
            denomination = denomination.value
        )
        val review = envelope.toDomainReview(
            requestContext = requestContext,
            executionPlan = preparedExecution.plan,
            approvalTarget = walletSession.walletAbiApprovalTarget(),
        )

        return PreparedWalletAbiReview(
            review = review,
            reviewLook = preparedExecution.toReviewLook(method = envelope.method),
            preparedExecution = preparedExecution,
        )
    }

    private suspend fun buildUnresolvedProviderReview(
        envelope: WalletAbiParsedEnvelope,
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?,
        requestFamily: WalletAbiRequestFamily,
    ): PreparedWalletAbiReview {
        val request = envelope.requireTxCreateRequest()
        validateProviderRequestShape(request = request, requestFamily = requestFamily)
        val context = executionContextResolver.resolveDirect(
            session = walletSession,
            requestNetwork = request.network,
            preferredAccountId = selectedAccountId,
        )
        eligibleAccounts = context.accounts
        val review = envelope.toProviderDomainReview(
            requestContext = requestContext,
            accounts = context.accounts,
            selectedAccount = context.primaryAccount,
            requestFamily = requestFamily,
            resolutionState = WalletAbiResolutionState.REQUIRED,
            approvalTarget = context.signerBackend.walletAbiApprovalTarget(),
        )

        return PreparedWalletAbiReview(
            review = review,
            reviewLook = buildProviderReviewLook(
                context = context,
                request = request,
                requestFamily = requestFamily,
                resolutionState = WalletAbiResolutionState.REQUIRED,
                preview = null,
            ),
            preparedExecution = null,
            providerPreparedExecution = null,
        )
    }

    private suspend fun buildResolvedProviderReview(
        envelope: WalletAbiParsedEnvelope,
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?,
        requestFamily: WalletAbiRequestFamily,
    ): PreparedWalletAbiReview {
        val request = envelope.requireTxCreateRequest()
        validateProviderRequestShape(request = request, requestFamily = requestFamily)
        val context = executionContextResolver.resolveDirect(
            session = walletSession,
            requestNetwork = request.network,
            preferredAccountId = selectedAccountId,
        )
        eligibleAccounts = context.accounts

        val providerEvaluation = providerRunner.evaluate(
            context = context,
            request = request,
        )
        val response = providerEvaluation.response
        val preview = response.preview
            ?: throw WalletAbiProviderRequestException(
                error = WalletAbiFlowError(
                    kind = WalletAbiFlowErrorKind.EXECUTION_FAILURE,
                    phase = WalletAbiFlowPhase.LOADING,
                    message = "Wallet ABI provider did not return an exact preview",
                    retryable = true,
                )
            )
        if (response.status != WalletAbiProviderStatus.OK) {
            val providerError = response.error
            throw WalletAbiProviderRequestException(
                error = WalletAbiFlowError(
                    kind = when {
                        providerError?.message?.contains("invalid", ignoreCase = true) == true ->
                            WalletAbiFlowErrorKind.INVALID_REQUEST
                        else -> WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST
                    },
                    phase = WalletAbiFlowPhase.LOADING,
                    message = providerError?.message ?: "Wallet ABI provider could not resolve the request",
                    retryable = false,
                )
            )
        }
        val resolvedRequest = request.withResolvedOutputs(preview)

        val providerPreparedExecution = WalletAbiProviderPreparedExecution(
            context = context,
            requestFamily = requestFamily,
            request = resolvedRequest.copy(broadcast = false),
            requestSession = providerEvaluation.requestSession,
            preview = preview,
        )
        val review = envelope.toProviderDomainReview(
            requestContext = requestContext,
            accounts = context.accounts,
            selectedAccount = context.primaryAccount,
            parsedRequest = WalletAbiParsedRequest.TxCreate(resolvedRequest),
            requestFamily = requestFamily,
            resolutionState = WalletAbiResolutionState.READY,
            approvalTarget = context.signerBackend.walletAbiApprovalTarget(),
        )

        return PreparedWalletAbiReview(
            review = review,
            reviewLook = buildProviderReviewLook(
                context = context,
                request = request,
                requestFamily = requestFamily,
                resolutionState = WalletAbiResolutionState.READY,
                preview = preview,
            ),
            preparedExecution = null,
            providerPreparedExecution = providerPreparedExecution,
        )
    }

    private suspend fun buildProviderReviewLook(
        context: WalletAbiExecutionContext,
        request: WalletAbiTxCreateRequest,
        requestFamily: WalletAbiRequestFamily,
        resolutionState: WalletAbiResolutionState,
        preview: WalletAbiProviderRequestPreview?,
    ): WalletAbiReviewLook {
        val selectedAccount = context.primaryAccount
        val requestedOutputCount = request.params.outputs.size
        val outputs = if (preview == null) {
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
                    recipientScript = output.scriptHex(),
                )
            }
        } else {
            preview.outputs.take(requestedOutputCount).mapIndexed { index, output ->
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
            }
        }
        val primaryOutput = outputs.firstOrNull() ?: WalletAbiReviewOutputLook(
            address = "Wallet ABI output",
            amount = "n/a",
            amountFiat = null,
            assetId = selectedAccount.network.policyAsset,
            recipientScript = null,
        )
        val assetImpacts = preview?.assetDeltas.orEmpty().map { delta ->
            WalletAbiReviewAssetImpactLook(
                assetId = delta.assetId,
                walletDelta = formatWalletAbiSignedAmount(
                    amountSat = delta.walletDeltaSat,
                    assetId = delta.assetId,
                    account = selectedAccount,
                ),
            )
        }
        val statusMessage = when (resolutionState) {
            WalletAbiResolutionState.REQUIRED ->
                "Resolve the request to review exact asset ids, wallet deltas, and final fees before approval."
            WalletAbiResolutionState.READY ->
                "Exact Wallet ABI review is ready. Approval will broadcast the resolved transaction."
            WalletAbiResolutionState.NOT_REQUIRED ->
                null
        }
        val warnings = buildList {
            if (resolutionState == WalletAbiResolutionState.REQUIRED) {
                add("Issued and reissued asset ids are deferred until resolution completes.")
            }
            if (requestFamily == WalletAbiRequestFamily.REISSUANCE) {
                request.params.inputs.firstOrNull()
                    ?.walletFilterExactAssetIdOrNull()
                    ?.let { add("Reissuance consumes wallet token asset $it.") }
            }
            addAll(preview?.warnings.orEmpty())
        }.distinct()
        val primaryAssetId = primaryOutput.assetId.ifBlank { selectedAccount.network.policyAsset }
        val feeRate = request.params.feeRateSatKvb
            ?.toLong()
            ?.takeIf { it > 0L }
            ?.feeRateWithUnit()
        val transactionConfirmation = preview?.toTransactionConfirmation(
            request = request,
            selectedAccount = selectedAccount,
            requestedOutputs = outputs,
            formatAmount = { amountSat, assetId ->
                formatWalletAbiAmount(
                    amountSat = amountSat,
                    assetId = assetId,
                    account = selectedAccount,
                )
            },
        )

        return WalletAbiReviewLook(
            accountAssetBalance = selectedAccount.accountAssetBalance,
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
            transactionConfirmation = transactionConfirmation?.copy(feeRate = feeRate),
            requestFamily = requestFamily,
            resolutionState = resolutionState,
            statusMessage = statusMessage,
            warnings = warnings,
            assetImpacts = assetImpacts,
            canResolve = resolutionState == WalletAbiResolutionState.REQUIRED,
            canApprove = resolutionState == WalletAbiResolutionState.READY,
        )
    }

    private suspend fun formatWalletAbiAmount(
        amountSat: Long,
        assetId: String?,
        account: Account,
    ): String {
        val normalizedAssetId = assetId?.takeIf { it.isNotBlank() }
        return amountSat.toAmountLook(
            session = walletSession,
            assetId = normalizedAssetId,
            denomination = normalizedAssetId?.takeIf { it == account.network.policyAsset }?.let { denomination.value },
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

    private fun markReviewRefreshing(selectedAccountId: String?) {
        val currentReviewLook = mutableReviewLook.value ?: return
        activePreparedExecution = null
        val account = eligibleAccounts.firstOrNull { it.id == selectedAccountId }
            ?: eligibleAccounts.firstOrNull { it.id == activeReview?.selectedAccountId }
            ?: return
        mutableReviewLook.value = currentReviewLook.copy(
            accountAssetBalance = account.accountAssetBalance,
            networkName = account.network.name.ifBlank { currentReviewLook.networkName },
            isRefreshing = true
        )
    }

    private fun clearPreparedReview() {
        activeEnvelope = null
        activePreparedExecution = null
        activeProviderPreparedExecution = null
        activeReview = null
        mutableReviewLook.value = null
    }

    companion object {
        const val DEMO_REQUEST_ID = "wallet-abi-demo-request"
    }
}

private data class PreparedWalletAbiReview(
    val review: WalletAbiFlowReview,
    val reviewLook: WalletAbiReviewLook,
    val preparedExecution: WalletAbiPreparedExecution? = null,
    val providerPreparedExecution: WalletAbiProviderPreparedExecution? = null,
)

private data class WalletAbiProviderPreparedExecution(
    val context: WalletAbiExecutionContext,
    val requestFamily: WalletAbiRequestFamily,
    val request: WalletAbiTxCreateRequest,
    val requestSession: WalletAbiRequestSession,
    val preview: WalletAbiProviderRequestPreview,
)

private fun GdkSession.walletAbiApprovalTarget(): WalletAbiApprovalTarget {
    val device = device
    return if (isHardwareWallet && device?.isJade == true) {
        WalletAbiApprovalTarget.Jade(
            deviceName = device.name,
            deviceId = device.connectionIdentifier,
        )
    } else {
        WalletAbiApprovalTarget.Software
    }
}

private fun WalletAbiSignerBackend.walletAbiApprovalTarget(): WalletAbiApprovalTarget {
    return when (this) {
        WalletAbiSignerBackend.Software -> WalletAbiApprovalTarget.Software
        is WalletAbiSignerBackend.Jade -> WalletAbiApprovalTarget.Jade(
            deviceName = deviceName,
            deviceId = deviceId,
        )
    }
}

private class WalletAbiProviderRequestException(
    val error: WalletAbiFlowError,
) : IllegalStateException(error.message)

private fun WalletAbiRequestValidationError.toFlowError(): WalletAbiFlowError {
    val kind = when (this) {
        WalletAbiRequestValidationError.MalformedEnvelopeJson,
        WalletAbiRequestValidationError.MissingMethod,
        WalletAbiRequestValidationError.MissingParams,
        WalletAbiRequestValidationError.MalformedRequestParams,
        WalletAbiRequestValidationError.BlankRequestId,
        is WalletAbiRequestValidationError.BlankInputId,
        is WalletAbiRequestValidationError.DuplicateInputId,
        is WalletAbiRequestValidationError.BlankOutputId,
        is WalletAbiRequestValidationError.DuplicateOutputId,
        is WalletAbiRequestValidationError.NonPositiveOutputAmount,
        is WalletAbiRequestValidationError.InvalidFeeRate -> WalletAbiFlowErrorKind.INVALID_REQUEST

        is WalletAbiRequestValidationError.UnsupportedMethod,
        is WalletAbiRequestValidationError.UnsupportedAbiVersion,
        is WalletAbiRequestValidationError.UnsupportedNetwork -> WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST
    }

    return WalletAbiFlowError(
        kind = kind,
        phase = WalletAbiFlowPhase.LOADING,
        message = message,
        retryable = false
    )
}

private fun Throwable.toWalletAbiFlowError(
    phase: WalletAbiFlowPhase,
    sessionConnected: Boolean
): WalletAbiFlowError {
    return when (this) {
        is WalletAbiProviderRequestException -> error
        is WalletAbiExecutionContextException -> {
            val isNetworkFailure = !sessionConnected || message.isNullOrBlank().not() && message!!.isLikelyNetworkFailure()
            WalletAbiFlowError(
                kind = if (isNetworkFailure) {
                    WalletAbiFlowErrorKind.NETWORK_FAILURE
                } else {
                    WalletAbiFlowErrorKind.EXECUTION_FAILURE
                },
                phase = phase,
                message = message ?: "Wallet ABI execution context is unavailable",
                retryable = isNetworkFailure,
            )
        }
        is WalletAbiExecutionValidationException -> {
            val kind = when (error) {
                WalletAbiExecutionValidationError.SessionNotConnected -> WalletAbiFlowErrorKind.NETWORK_FAILURE
                WalletAbiExecutionValidationError.HardwareWalletUnsupported,
                is WalletAbiExecutionValidationError.UnsupportedRequestType,
                WalletAbiExecutionValidationError.BroadcastRequired,
                is WalletAbiExecutionValidationError.ExplicitInputsUnsupported,
                is WalletAbiExecutionValidationError.OutputCountUnsupported,
                WalletAbiExecutionValidationError.LockTimeUnsupported,
                is WalletAbiExecutionValidationError.InvalidFeeRate,
                WalletAbiExecutionValidationError.MissingAssetId,
                is WalletAbiExecutionValidationError.UnsupportedAsset,
                WalletAbiExecutionValidationError.WalletOwnedOutputUnsupported,
                WalletAbiExecutionValidationError.OutputLockUnsupported -> WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST

                WalletAbiExecutionValidationError.MissingMnemonic,
                is WalletAbiExecutionValidationError.NoEligibleAccount,
                is WalletAbiExecutionValidationError.SelectedAccountUnavailable -> WalletAbiFlowErrorKind.EXECUTION_FAILURE
            }
            WalletAbiFlowError(
                kind = kind,
                phase = phase,
                message = error.message,
                retryable = kind == WalletAbiFlowErrorKind.NETWORK_FAILURE ||
                    kind == WalletAbiFlowErrorKind.EXECUTION_FAILURE
            )
        }

        else -> {
            val errorMessage = message ?: "Wallet ABI execution failed"
            val isNetworkFailure = !sessionConnected || errorMessage.isLikelyNetworkFailure()

            WalletAbiFlowError(
                kind = if (isNetworkFailure) {
                    WalletAbiFlowErrorKind.NETWORK_FAILURE
                } else {
                    WalletAbiFlowErrorKind.EXECUTION_FAILURE
                },
                phase = phase,
                message = errorMessage,
                retryable = true
            )
        }
    }
}

private fun String.isLikelyNetworkFailure(): Boolean {
    val normalized = lowercase()
    return "network" in normalized ||
        "transport" in normalized ||
        "connection" in normalized ||
        "disconnected" in normalized
}

private fun String?.toWalletAbiMethodOrNull(): WalletAbiMethod? {
    return WalletAbiMethod.entries.firstOrNull { it.wireValue == this }
}

private fun WalletAbiPreparedExecution.toReviewLook(
    method: WalletAbiMethod
): WalletAbiReviewLook {
    val request = plan.request.request
    val selectedAccount = plan.selectedAccount
    val recipients = confirmation.utxos.orEmpty().filterNot { it.isChange }
    val outputs = plan.outputs.mapIndexed { index, output ->
        val recipient = recipients.getOrNull(index)
        WalletAbiReviewOutputLook(
            address = recipient?.address ?: output.destinationAddress,
            amount = recipient?.amount ?: output.amountSat.toString(),
            amountFiat = recipient?.amountExchange,
            assetId = output.assetId,
            recipientScript = output.recipientScript
        )
    }
    val primaryOutput = outputs.first()
    val primaryAssetId = plan.outputs.first().assetId

    return WalletAbiReviewLook(
        accountAssetBalance = selectedAccount.accountAssetBalance,
        outputs = outputs,
        recipientAddress = primaryOutput.address,
        amount = primaryOutput.amount,
        amountFiat = primaryOutput.amountFiat,
        assetName = selectedAccount.accountAsset.asset.name ?: primaryAssetId,
        assetTicker = selectedAccount.accountAsset.asset.ticker,
        assetId = primaryAssetId,
        networkName = selectedAccount.network.name.ifBlank { request.network.wireValue },
        networkWireValue = request.network.wireValue,
        method = method.wireValue,
        abiVersion = request.abiVersion,
        requestId = request.requestId,
        broadcast = request.broadcast,
        recipientScript = primaryOutput.recipientScript,
        transactionConfirmation = confirmation,
        requestFamily = when (plan.outputs.size) {
            1 -> WalletAbiRequestFamily.PAYMENT
            else -> WalletAbiRequestFamily.SPLIT
        },
        resolutionState = WalletAbiResolutionState.NOT_REQUIRED,
        canResolve = false,
        canApprove = true,
    )
}

private fun WalletAbiParsedEnvelope.toDomainReview(
    requestContext: WalletAbiStartRequestContext,
    executionPlan: WalletAbiExecutionPlan,
    approvalTarget: WalletAbiApprovalTarget,
): WalletAbiFlowReview {
    return WalletAbiFlowReview(
        requestContext = requestContext,
        method = method.wireValue,
        title = "Wallet ABI payment",
        message = "Approve a Wallet ABI request",
        accounts = executionPlan.accounts.map { account ->
            WalletAbiAccountOption(
                accountId = account.id,
                name = account.name
            )
        },
        selectedAccountId = executionPlan.selectedAccount.id,
        approvalTarget = approvalTarget,
        parsedRequest = request,
        executionDetails = WalletAbiExecutionDetails(
            destinationAddress = executionPlan.outputs.first().destinationAddress,
            amountSat = executionPlan.outputs.sumOf { it.amountSat },
            assetId = executionPlan.outputs.first().assetId,
            network = executionPlan.request.request.network.wireValue,
            feeRate = executionPlan.feeRate,
            requestFamily = when (executionPlan.outputs.size) {
                1 -> WalletAbiRequestFamily.PAYMENT
                else -> WalletAbiRequestFamily.SPLIT
            },
            resolutionState = WalletAbiResolutionState.NOT_REQUIRED,
            outputCount = executionPlan.outputs.size,
        )
    )
}

private fun com.blockstream.domain.walletabi.request.WalletAbiOutput.scriptHex(): String? {
    val lockObject = lock as? JsonObject ?: return null
    if (lockObject["type"]?.jsonPrimitive?.content?.trim()?.lowercase() != "script") {
        return null
    }

    return lockObject["script"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
}

private fun WalletAbiParsedEnvelope.providerRequestFamilyOrNull(): WalletAbiRequestFamily? {
    val request = (request as? WalletAbiParsedRequest.TxCreate)?.request ?: return null
    val outputAssetTypes = request.params.outputs.mapNotNull { it.assetTypeOrNull() }.toSet()
    return when {
        "re_issuance_asset" in outputAssetTypes ||
            request.params.inputs.any { it.issuanceKindOrNull() == "reissue" } -> WalletAbiRequestFamily.REISSUANCE

        "new_issuance_asset" in outputAssetTypes ||
            "new_issuance_token" in outputAssetTypes ||
            request.params.inputs.any { it.issuanceKindOrNull() == "new" } -> WalletAbiRequestFamily.ISSUANCE

        else -> null
    }
}

private fun WalletAbiParsedEnvelope.requireTxCreateRequest(): WalletAbiTxCreateRequest {
    return (request as? WalletAbiParsedRequest.TxCreate)?.request
        ?: throw WalletAbiProviderRequestException(
            error = WalletAbiFlowError(
                kind = WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST,
                phase = WalletAbiFlowPhase.LOADING,
                message = "Wallet ABI provider execution supports tx_create requests only",
                retryable = false,
            )
        )
}

private fun validateProviderRequestShape(
    request: WalletAbiTxCreateRequest,
    requestFamily: WalletAbiRequestFamily,
) {
    if (!request.broadcast) {
        throw WalletAbiProviderRequestException(
            error = WalletAbiFlowError(
                kind = WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST,
                phase = WalletAbiFlowPhase.LOADING,
                message = "Wallet ABI provider execution currently requires broadcast=true",
                retryable = false,
            )
        )
    }
    if (request.params.lockTime != null) {
        throw WalletAbiProviderRequestException(
            error = WalletAbiFlowError(
                kind = WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST,
                phase = WalletAbiFlowPhase.LOADING,
                message = "Wallet ABI provider execution does not support lock_time yet",
                retryable = false,
            )
        )
    }

    when (requestFamily) {
        WalletAbiRequestFamily.ISSUANCE -> {
            if (request.params.inputs.size != 1) {
                throw providerUnsupported("Wallet ABI issuance currently supports exactly one funding input")
            }
            val input = request.params.inputs.single()
            if (input.issuanceKindOrNull() != "new") {
                throw providerUnsupported("Wallet ABI issuance requires issuance.kind = new")
            }
            if (input.utxoSourceKindOrNull() != "wallet") {
                throw providerUnsupported("Wallet ABI issuance requires wallet-selected funding inputs")
            }
            if (request.params.outputs.size != 2) {
                throw providerUnsupported("Wallet ABI issuance currently supports exactly two wallet outputs")
            }
            val outputTypes = request.params.outputs.map { it.assetTypeOrNull() }
            if (!outputTypes.contains("new_issuance_asset") || !outputTypes.contains("new_issuance_token")) {
                throw providerUnsupported("Wallet ABI issuance requires issued-asset and reissuance-token outputs")
            }
            request.params.outputs.forEach { output ->
                if (output.assetInputIndexOrNull() != 0 || output.lockTypeOrNull() != "wallet" || output.blinderTypeOrNull() != "wallet") {
                    throw providerUnsupported("Wallet ABI issuance outputs must be wallet-locked, wallet-blinded, and reference input 0")
                }
            }
        }

        WalletAbiRequestFamily.REISSUANCE -> {
            if (request.params.inputs.size != 1) {
                throw providerUnsupported("Wallet ABI reissuance currently supports exactly one wallet token input")
            }
            val input = request.params.inputs.single()
            if (input.issuanceKindOrNull() != "reissue") {
                throw providerUnsupported("Wallet ABI reissuance requires issuance.kind = reissue")
            }
            if (input.utxoSourceKindOrNull() != "wallet") {
                throw providerUnsupported("Wallet ABI reissuance requires wallet-selected token inputs")
            }
            if (input.walletFilterExactAssetIdOrNull().isNullOrBlank()) {
                throw providerUnsupported("Wallet ABI reissuance requires an exact wallet token asset filter")
            }
            if (request.params.outputs.size != 1) {
                throw providerUnsupported("Wallet ABI reissuance currently supports exactly one wallet output")
            }
            val output = request.params.outputs.single()
            if (output.assetTypeOrNull() != "re_issuance_asset" ||
                output.assetInputIndexOrNull() != 0 ||
                output.lockTypeOrNull() != "wallet" ||
                output.blinderTypeOrNull() != "wallet"
            ) {
                throw providerUnsupported("Wallet ABI reissuance requires one wallet-locked re_issuance_asset output referencing input 0")
            }
        }

        else -> Unit
    }
}

private fun providerUnsupported(message: String): WalletAbiProviderRequestException {
    return WalletAbiProviderRequestException(
        error = WalletAbiFlowError(
            kind = WalletAbiFlowErrorKind.UNSUPPORTED_REQUEST,
            phase = WalletAbiFlowPhase.LOADING,
            message = message,
            retryable = false,
        )
    )
}

private fun WalletAbiParsedEnvelope.toProviderDomainReview(
    requestContext: WalletAbiStartRequestContext,
    accounts: List<Account>,
    selectedAccount: Account,
    parsedRequest: WalletAbiParsedRequest = request,
    requestFamily: WalletAbiRequestFamily,
    resolutionState: WalletAbiResolutionState,
    approvalTarget: WalletAbiApprovalTarget = WalletAbiApprovalTarget.Software,
): WalletAbiFlowReview {
    val txRequest = requireTxCreateRequest()
    val primaryOutput = txRequest.params.outputs.firstOrNull()
    return WalletAbiFlowReview(
        requestContext = requestContext,
        method = method.wireValue,
        title = when (requestFamily) {
            WalletAbiRequestFamily.ISSUANCE -> "Wallet ABI issuance"
            WalletAbiRequestFamily.REISSUANCE -> "Wallet ABI reissuance"
            else -> "Wallet ABI request"
        },
        message = when (resolutionState) {
            WalletAbiResolutionState.REQUIRED -> "Resolve the request before approving this Wallet ABI action"
            WalletAbiResolutionState.READY -> "Review the exact Wallet ABI transaction before approval"
            WalletAbiResolutionState.NOT_REQUIRED -> "Approve a Wallet ABI request"
        },
        accounts = accounts.map { account ->
            WalletAbiAccountOption(accountId = account.id, name = account.name)
        },
        selectedAccountId = selectedAccount.id,
        approvalTarget = approvalTarget,
        parsedRequest = parsedRequest,
        executionDetails = WalletAbiExecutionDetails(
            destinationAddress = primaryOutput?.summaryAddress() ?: "Wallet ABI output",
            amountSat = txRequest.params.outputs.sumOf { it.amountSat },
            assetId = primaryOutput?.explicitAssetIdOrNull() ?: "unresolved",
            network = txRequest.network.wireValue,
            feeRate = txRequest.params.feeRateSatKvb?.toLong()?.takeIf { it > 0L },
            requestFamily = requestFamily,
            resolutionState = resolutionState,
            outputCount = txRequest.params.outputs.size,
        ),
    )
}

private fun WalletAbiTxCreateRequest.withResolvedOutputs(
    preview: WalletAbiProviderRequestPreview,
): WalletAbiTxCreateRequest {
    val resolvedOutputs = params.outputs.mapIndexed { index, output ->
        val previewOutput = preview.outputs.getOrNull(index) ?: return@mapIndexed output
        output.copy(
            asset = buildJsonObject {
                put("asset_id", JsonPrimitive(previewOutput.assetId))
            },
            lock = buildJsonObject {
                put("type", JsonPrimitive("script"))
                put("script", JsonPrimitive(previewOutput.scriptPubkey))
            },
        )
    }
    return copy(params = params.copy(outputs = resolvedOutputs))
}

private suspend fun WalletAbiProviderRequestPreview.toTransactionConfirmation(
    request: WalletAbiTxCreateRequest,
    selectedAccount: Account,
    requestedOutputs: List<WalletAbiReviewOutputLook>,
    formatAmount: suspend (Long, String?) -> String,
): com.blockstream.data.transaction.TransactionConfirmation {
    val feeOutput = outputs.drop(request.params.outputs.size)
        .firstOrNull { it.kind == WalletAbiProviderPreviewOutputKind.FEE }
    val fee = feeOutput?.let { output ->
        formatAmount(output.amountSat, output.assetId)
    }
    val feeAssetId = feeOutput?.assetId
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
        feeAssetId = feeAssetId,
        total = total,
    )
}

private fun WalletAbiInput.issuanceKindOrNull(): String? {
    val issuanceObject = issuance as? JsonObject ?: return null
    return issuanceObject["kind"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
}

private fun WalletAbiInput.utxoSourceKindOrNull(): String? {
    val utxoSourceObject = utxoSource as? JsonObject ?: return null
    return when {
        "wallet" in utxoSourceObject -> "wallet"
        "provided" in utxoSourceObject -> "provided"
        "type" in utxoSourceObject -> utxoSourceObject["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
        else -> null
    }
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

private fun WalletAbiOutput.assetInputIndexOrNull(): Int? {
    val assetObject = asset as? JsonObject ?: return null
    return assetObject["input_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        ?: assetObject["inputIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
}

private fun WalletAbiOutput.lockTypeOrNull(): String? {
    val lockObject = lock as? JsonObject ?: return null
    return lockObject["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
}

private fun WalletAbiOutput.blinderTypeOrNull(): String? {
    return when (val blinderValue = blinder) {
        is JsonPrimitive -> blinderValue.contentOrNull?.trim()?.lowercase()
        is JsonObject -> blinderValue["type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
        else -> null
    }
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

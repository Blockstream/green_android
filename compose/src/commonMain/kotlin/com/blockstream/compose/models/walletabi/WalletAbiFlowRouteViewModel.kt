package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.walletabi.request.WalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
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
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.DefaultWalletAbiRequestParser
import com.blockstream.domain.walletabi.request.WalletAbiMethod
import com.blockstream.domain.walletabi.request.WalletAbiParsedEnvelope
import com.blockstream.domain.walletabi.request.WalletAbiRequestParseResult
import com.blockstream.domain.walletabi.request.WalletAbiRequestValidationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class WalletAbiFlowRouteViewModel(
    greenWallet: GreenWallet,
    private val store: WalletAbiFlowStore,
    private val snapshotRepository: WalletAbiFlowSnapshotRepository,
    private val walletSession: GdkSession,
    private val requestSource: WalletAbiDemoRequestSource = DefaultWalletAbiDemoRequestSource(),
    private val executionPlanner: WalletAbiExecutionPlanner = DefaultWalletAbiExecutionPlanner(),
    private val executionRunner: WalletAbiExecutionRunner = DefaultWalletAbiExecutionRunner(),
    private val reviewPreviewer: WalletAbiReviewPreviewer = DefaultWalletAbiReviewPreviewer(),
    private val loadingTimeoutMillis: Long = 15_000L,
    private val submissionTimeoutMillis: Long = 60_000L
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    private val requestParser = DefaultWalletAbiRequestParser()
    private val mutableReviewLook = MutableStateFlow<WalletAbiReviewLook?>(null)
    private var loadingJob: Job? = null
    private var reviewRefreshJob: Job? = null
    private var submissionJob: Job? = null
    private var activeEnvelope: WalletAbiParsedEnvelope? = null
    private var activeReview: WalletAbiFlowReview? = null
    private var activePreparedExecution: WalletAbiPreparedExecution? = null
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
        startFlow(greenWallet = greenWallet)
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
                WalletAbiFlowPhase.SUBMISSION -> {
                    submissionJob?.cancel()
                    store.dispatch(
                        WalletAbiFlowIntent.OnExecutionEvent(
                            WalletAbiExecutionEvent.Cancelled(WalletAbiCancelledReason.UserCancelled)
                        )
                    )
                }
                else -> Unit
            }
            is WalletAbiFlowOutput.StartResolution -> {
                launchReviewRefresh(
                    requestContext = output.command.requestContext,
                    selectedAccountId = output.command.selectedAccountId
                )
            }

            is WalletAbiFlowOutput.StartSubmission -> {
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
            is WalletAbiFlowOutput.StartApproval -> Unit
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

    private fun launchReviewRefresh(
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?
    ) {
        reviewRefreshJob?.cancel()
        reviewRefreshJob = viewModelScope.launch {
            try {
                refreshPreparedReview(
                    requestContext = requestContext,
                    selectedAccountId = selectedAccountId
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
                selectedAccountId = null
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
        selectedAccountId: String?
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
                selectedAccountId = selectedAccountId
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
            if (intent is WalletAbiFlowIntent.SelectAccount) {
                launchReviewRefresh(
                    requestContext = (store.state.value as? WalletAbiFlowState.RequestLoaded)?.review?.requestContext
                        ?: return@launch,
                    selectedAccountId = intent.accountId
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

    private fun startFlow(greenWallet: GreenWallet) {
        viewModelScope.launch {
            store.dispatch(
                WalletAbiFlowIntent.Start(
                    requestContext = WalletAbiStartRequestContext(
                        requestId = DEMO_REQUEST_ID,
                        walletId = greenWallet.id
                    )
                )
            )
        }
    }

    private suspend fun buildPreparedReview(
        envelope: WalletAbiParsedEnvelope,
        requestContext: WalletAbiStartRequestContext,
        selectedAccountId: String?
    ): PreparedWalletAbiReview {
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
            executionPlan = preparedExecution.plan
        )

        return PreparedWalletAbiReview(
            review = review,
            reviewLook = preparedExecution.toReviewLook(method = envelope.method),
            preparedExecution = preparedExecution
        )
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
    val preparedExecution: WalletAbiPreparedExecution
)

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

private fun WalletAbiPreparedExecution.toReviewLook(
    method: WalletAbiMethod
): WalletAbiReviewLook {
    val request = plan.request.request
    val selectedAccount = plan.selectedAccount
    val recipient = confirmation.utxos?.firstOrNull()

    return WalletAbiReviewLook(
        accountAssetBalance = selectedAccount.accountAssetBalance,
        recipientAddress = recipient?.address ?: plan.destinationAddress,
        amount = recipient?.amount ?: plan.amountSat.toString(),
        amountFiat = recipient?.amountExchange,
        assetName = selectedAccount.accountAsset.asset.name ?: plan.assetId,
        assetTicker = selectedAccount.accountAsset.asset.ticker,
        assetId = plan.assetId,
        networkName = selectedAccount.network.name.ifBlank { request.network.wireValue },
        networkWireValue = request.network.wireValue,
        method = method.wireValue,
        abiVersion = request.abiVersion,
        requestId = request.requestId,
        broadcast = request.broadcast,
        recipientScript = request.params.outputs.singleOrNull()?.scriptHex(),
        transactionConfirmation = confirmation
    )
}

private fun WalletAbiParsedEnvelope.toDomainReview(
    requestContext: WalletAbiStartRequestContext,
    executionPlan: WalletAbiExecutionPlan
): WalletAbiFlowReview {
    return WalletAbiFlowReview(
        requestContext = requestContext,
        title = "Wallet ABI payment",
        message = "Approve a Wallet ABI request",
        accounts = executionPlan.accounts.map { account ->
            WalletAbiAccountOption(
                accountId = account.id,
                name = account.name
            )
        },
        selectedAccountId = executionPlan.selectedAccount.id,
        approvalTarget = WalletAbiApprovalTarget.Software,
        parsedRequest = request,
        executionDetails = WalletAbiExecutionDetails(
            destinationAddress = executionPlan.destinationAddress,
            amountSat = executionPlan.amountSat,
            assetId = executionPlan.assetId,
            network = executionPlan.request.request.network.wireValue,
            feeRate = executionPlan.feeRate
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

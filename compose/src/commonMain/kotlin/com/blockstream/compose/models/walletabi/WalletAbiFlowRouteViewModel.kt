package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.walletabi.request.WalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlan
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionValidationException
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionEvent
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionDetails
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiFlowError
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.DefaultWalletAbiRequestParser
import com.blockstream.domain.walletabi.request.WalletAbiParsedEnvelope
import com.blockstream.domain.walletabi.request.WalletAbiRequestParseResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletAbiFlowRouteViewModel(
    greenWallet: GreenWallet,
    private val store: WalletAbiFlowStore,
    private val snapshotRepository: WalletAbiFlowSnapshotRepository,
    private val walletSession: GdkSession,
    private val requestSource: WalletAbiDemoRequestSource = DefaultWalletAbiDemoRequestSource(),
    private val executionPlanner: WalletAbiExecutionPlanner = DefaultWalletAbiExecutionPlanner(),
    private val executionRunner: WalletAbiExecutionRunner = DefaultWalletAbiExecutionRunner()
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    private val requestParser = DefaultWalletAbiRequestParser()
    private var activeReview: WalletAbiFlowReview? = null

    override fun screenName(): String = "WalletAbiFlow"
    override val isLoginRequired: Boolean = false

    val state: StateFlow<WalletAbiFlowState> = store.state

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
                when (val parseResult = requestParser.parse(requestSource.loadRequestEnvelope(output.requestContext.requestId))) {
                    is WalletAbiRequestParseResult.Failure -> {
                        dispatchExecutionFailure(parseResult.error.message)
                    }

                    is WalletAbiRequestParseResult.Success -> {
                        val review = runCatching {
                            parseResult.envelope.toDomainReview(
                                requestContext = output.requestContext,
                                executionPlan = executionPlanner.plan(
                                    session = walletSession,
                                    request = parseResult.envelope.request
                                )
                            )
                        }.getOrElse { throwable ->
                            dispatchExecutionFailure(
                                when (throwable) {
                                    is WalletAbiExecutionValidationException -> throwable.error.message
                                    else -> throwable.message ?: "Wallet ABI request is unsupported"
                                }
                            )
                            return
                        }

                        activeReview = review
                        store.dispatch(
                            WalletAbiFlowIntent.OnExecutionEvent(
                                WalletAbiExecutionEvent.RequestLoaded(
                                    review = review
                                )
                            )
                        )
                    }
                }
            }

            is WalletAbiFlowOutput.PersistSnapshot -> {
                val snapshot = output.snapshot
                if (snapshot == null) {
                    snapshotRepository.clear(greenWallet.id)
                } else {
                    snapshotRepository.save(greenWallet.id, snapshot)
                }
            }

            is WalletAbiFlowOutput.StartResolution -> {
                val review = activeReview ?: (store.state.value as? WalletAbiFlowState.RequestLoaded)?.review ?: return
                val resolvedReview = review.copy(
                    selectedAccountId = output.command.selectedAccountId ?: review.selectedAccountId
                )
                activeReview = resolvedReview
                store.dispatch(
                    WalletAbiFlowIntent.OnExecutionEvent(
                        WalletAbiExecutionEvent.Resolved(
                            review = resolvedReview
                        )
                    )
                )
            }

            is WalletAbiFlowOutput.StartSubmission -> {
                val parsedRequest = activeReview?.parsedRequest
                    ?: return dispatchExecutionFailure("Wallet ABI request is not loaded")
                val executionPlan = runCatching {
                    executionPlanner.plan(
                        session = walletSession,
                        request = parsedRequest,
                        selectedAccountId = output.command.selectedAccountId
                    )
                }.getOrElse { throwable ->
                    dispatchExecutionFailure(
                        when (throwable) {
                            is WalletAbiExecutionValidationException -> throwable.error.message
                            else -> throwable.message ?: "Wallet ABI request is unsupported"
                        }
                    )
                    return
                }

                store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Submitted))

                runCatching {
                    executionRunner.execute(
                        session = walletSession,
                        plan = executionPlan,
                        twoFactorResolver = this
                    )
                }.onSuccess { result ->
                    store.dispatch(WalletAbiFlowIntent.OnExecutionEvent(WalletAbiExecutionEvent.Broadcasted))
                    store.dispatch(
                        WalletAbiFlowIntent.OnExecutionEvent(
                            WalletAbiExecutionEvent.RemoteResponseSent(
                                result = WalletAbiSuccessResult(
                                    requestId = executionPlan.request.request.requestId,
                                    txHash = result.txHash
                                )
                            )
                        )
                    )
                }.onFailure { throwable ->
                    dispatchExecutionFailure(throwable.message ?: "Wallet ABI execution failed")
                }
            }

            is WalletAbiFlowOutput.Complete -> Unit
            is WalletAbiFlowOutput.StartApproval -> Unit
        }
    }

    private suspend fun dispatchExecutionFailure(message: String) {
        store.dispatch(
            WalletAbiFlowIntent.OnExecutionEvent(
                WalletAbiExecutionEvent.Failed(
                    WalletAbiFlowError(message)
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

    companion object {
        const val DEMO_REQUEST_ID = "wallet-abi-demo-request"
    }
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

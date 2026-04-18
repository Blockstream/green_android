package com.blockstream.compose.models.walletabi

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.data.walletabi.flow.FakeWalletAbiSubmissionEvent
import com.blockstream.data.walletabi.flow.WalletAbiAccountOptionPayload
import com.blockstream.data.walletabi.flow.WalletAbiApprovalTargetPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowReviewPayload
import com.blockstream.domain.walletabi.flow.WalletAbiFlowIntent
import com.blockstream.domain.walletabi.flow.WalletAbiAccountOption
import com.blockstream.domain.walletabi.flow.WalletAbiApprovalTarget
import com.blockstream.domain.walletabi.flow.WalletAbiExecutionEvent
import com.blockstream.domain.walletabi.flow.WalletAbiFlowReview
import com.blockstream.domain.walletabi.flow.WalletAbiFlowOutput
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowState
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiFlowError
import com.blockstream.domain.walletabi.flow.WalletAbiStartRequestContext
import com.blockstream.domain.walletabi.flow.WalletAbiSuccessResult
import com.blockstream.domain.walletabi.request.DefaultWalletAbiRequestParser
import com.blockstream.domain.walletabi.request.WalletAbiMethod
import com.blockstream.domain.walletabi.request.WalletAbiParsedEnvelope
import com.blockstream.domain.walletabi.request.WalletAbiRequestParseResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletAbiFlowRouteViewModel(
    greenWallet: GreenWallet,
    private val store: WalletAbiFlowStore,
    private val snapshotRepository: WalletAbiFlowSnapshotRepository,
    private val driver: FakeWalletAbiFlowDriver
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    private val requestParser = DefaultWalletAbiRequestParser()

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
                when (val parseResult = requestParser.parse(driver.loadRequestEnvelope(output.requestContext.requestId))) {
                    is WalletAbiRequestParseResult.Failure -> {
                        store.dispatch(
                            WalletAbiFlowIntent.OnExecutionEvent(
                                WalletAbiExecutionEvent.Failed(
                                    WalletAbiFlowError(parseResult.error.message)
                                )
                            )
                        )
                    }

                    is WalletAbiRequestParseResult.Success -> {
                        store.dispatch(
                            WalletAbiFlowIntent.OnExecutionEvent(
                                WalletAbiExecutionEvent.RequestLoaded(
                                    review = parseResult.envelope.toDomainReview(output.requestContext)
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
                val review = (store.state.value as? WalletAbiFlowState.RequestLoaded)?.review ?: return
                val resolvedPayload = driver.resolveRequest(
                    review = WalletAbiFlowReviewPayload(
                        requestId = output.command.requestContext.requestId,
                        walletId = output.command.requestContext.walletId,
                        title = review.title,
                        message = review.message,
                        accounts = review.accounts.map { account ->
                            WalletAbiAccountOptionPayload(
                                accountId = account.accountId,
                                name = account.name
                            )
                        },
                        selectedAccountId = output.command.selectedAccountId,
                        approvalTarget = when (val approvalTarget = review.approvalTarget) {
                            is WalletAbiApprovalTarget.Jade -> WalletAbiApprovalTargetPayload(
                                kind = "jade",
                                deviceName = approvalTarget.deviceName,
                                deviceId = approvalTarget.deviceId
                            )

                            WalletAbiApprovalTarget.Software -> WalletAbiApprovalTargetPayload(
                                kind = "software"
                            )
                        },
                        parsedRequest = null
                    )
                )
                store.dispatch(
                    WalletAbiFlowIntent.OnExecutionEvent(
                        WalletAbiExecutionEvent.Resolved(
                            review = resolvedPayload.toDomainReview(
                                requestContext = output.command.requestContext,
                                parsedEnvelope = review.parsedRequest?.let { parsedRequest ->
                                    WalletAbiParsedEnvelope(
                                        id = null,
                                        method = WalletAbiMethod.PROCESS_REQUEST,
                                        request = parsedRequest
                                    )
                                }
                            )
                        )
                    )
                )
            }

            is WalletAbiFlowOutput.StartSubmission -> {
                driver.submitRequest(
                    requestId = output.command.requestContext.requestId
                ).collect { event ->
                    store.dispatch(
                        WalletAbiFlowIntent.OnExecutionEvent(
                            when (event) {
                                FakeWalletAbiSubmissionEvent.Broadcasted -> WalletAbiExecutionEvent.Broadcasted
                                is FakeWalletAbiSubmissionEvent.RemoteResponseSent -> WalletAbiExecutionEvent.RemoteResponseSent(
                                    result = WalletAbiSuccessResult(
                                        requestId = event.result.requestId,
                                        responseId = event.result.responseId
                                    )
                                )

                                FakeWalletAbiSubmissionEvent.Submitted -> WalletAbiExecutionEvent.Submitted
                            }
                        )
                    )
                }
            }

            is WalletAbiFlowOutput.Complete -> Unit
            is WalletAbiFlowOutput.StartApproval -> Unit
        }
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

private fun WalletAbiFlowReviewPayload.toDomainReview(
    requestContext: WalletAbiStartRequestContext,
    parsedEnvelope: WalletAbiParsedEnvelope? = null
): WalletAbiFlowReview {
    return WalletAbiFlowReview(
        requestContext = requestContext,
        title = title,
        message = message,
        accounts = accounts.map { account ->
            WalletAbiAccountOption(
                accountId = account.accountId,
                name = account.name
            )
        },
        selectedAccountId = selectedAccountId,
        approvalTarget = when (approvalTarget.kind) {
            "jade" -> WalletAbiApprovalTarget.Jade(
                deviceName = approvalTarget.deviceName,
                deviceId = approvalTarget.deviceId
            )

            else -> WalletAbiApprovalTarget.Software
        },
        parsedRequest = parsedEnvelope?.request
    )
}

private fun WalletAbiParsedEnvelope.toDomainReview(
    requestContext: WalletAbiStartRequestContext
): WalletAbiFlowReview {
    return WalletAbiFlowReview(
        requestContext = requestContext,
        title = "Demo payment",
        message = "Approve a fake Wallet ABI request",
        accounts = listOf(
            WalletAbiAccountOption(
                accountId = "fake-account-1",
                name = "Main account"
            )
        ),
        selectedAccountId = "fake-account-1",
        approvalTarget = WalletAbiApprovalTarget.Software,
        parsedRequest = request
    )
}

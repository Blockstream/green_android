package com.blockstream.data.walletabi.walletconnect

import co.touchlab.kermit.Logger
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.walletabi.provider.WalletAbiEsploraHttpClient
import com.blockstream.data.walletabi.provider.WalletAbiWalletSnapshotSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import lwk.Mnemonic
import lwk.Network as LwkNetwork
import lwk.Signer
import lwk.SignerMetaLink
import lwk.WalletAbiProvider
import lwk.WalletAbiSignerContext
import lwk.WalletAbiWalletConnectCoordinator
import lwk.WalletAbiWalletConnectOverlayKind
import lwk.WalletAbiWalletConnectRpcErrorKind
import lwk.WalletAbiWalletConnectSessionInfo
import lwk.WalletAbiWalletConnectSessionProposal
import lwk.WalletAbiWalletConnectSessionRequest
import lwk.WalletAbiWalletConnectTransportAction
import lwk.WalletAbiWalletConnectTransportActionKind
import lwk.WalletAbiWalletConnectUiState
import lwk.WalletBroadcasterLink
import lwk.WalletOutputAllocatorLink
import lwk.WalletPrevoutResolverLink
import lwk.WalletReceiveAddressProviderLink
import lwk.WalletRuntimeDepsLink
import lwk.WalletSessionFactoryLink

sealed interface WalletAbiWalletConnectActionOutcome {
    data object SessionApproved : WalletAbiWalletConnectActionOutcome
    data object SessionRejected : WalletAbiWalletConnectActionOutcome
    data object SessionDisconnected : WalletAbiWalletConnectActionOutcome
    data class RequestSucceeded(val resultJson: String) : WalletAbiWalletConnectActionOutcome
    data class RequestRejected(val message: String) : WalletAbiWalletConnectActionOutcome
}

data class WalletAbiWalletConnectState(
    val uiState: WalletAbiWalletConnectUiState = WalletAbiWalletConnectUiState(
        activeSessions = emptyList(),
        currentOverlay = null,
        queuedOverlayCount = 0u,
        lastError = null,
        pendingActionCount = 0u,
    ),
    val bridgeError: String? = null,
    val isPairing: Boolean = false,
    val preparingRequest: WalletAbiWalletConnectPreparingRequest? = null,
) {
    val lastError: String?
        get() = bridgeError ?: uiState.lastError
}

data class WalletAbiWalletConnectPreparingRequest(
    val topic: String,
    val requestId: String,
    val method: String,
)

interface WalletAbiWalletConnectManaging {
    fun state(walletId: String): StateFlow<WalletAbiWalletConnectState>
    suspend fun bind(greenWallet: GreenWallet, session: GdkSession)
    suspend fun pair(greenWallet: GreenWallet, session: GdkSession, input: String)
    suspend fun approveCurrentOverlay(walletId: String): WalletAbiWalletConnectActionOutcome?
    suspend fun rejectCurrentOverlay(walletId: String): WalletAbiWalletConnectActionOutcome?
    suspend fun disconnectActiveSession(walletId: String): WalletAbiWalletConnectActionOutcome?
    suspend fun clearError(walletId: String)
}

class WalletAbiWalletConnectManager(
    private val applicationScope: ApplicationScope,
    private val snapshotStore: WalletAbiWalletConnectSnapshotStore,
    private val bridge: WalletAbiWalletConnectBridge,
    private val esploraHttpClient: WalletAbiEsploraHttpClient,
) : WalletAbiWalletConnectManaging {
    private val logger = Logger.withTag("WalletAbiWalletConnectManager")
    private val runtimeMutex = Mutex()
    private val bridgeRequestMutex = Mutex()
    private val inFlightBridgeRequests = linkedSetOf<WalletAbiWalletConnectRequestKey>()
    private val runtimes = linkedMapOf<String, WalletRuntime>()
    private val stateFlows = mutableMapOf<String, MutableStateFlow<WalletAbiWalletConnectState>>()
    private var pendingPairWalletId: String? = null

    init {
        bridge.setListener(
            object : WalletAbiWalletConnectBridgeListener {
                override fun onSessionProposal(proposal: WalletAbiWalletConnectSessionProposal) {
                    applicationScope.launch(Dispatchers.IO) {
                        runCatching {
                            handleSessionProposal(proposal)
                        }.onFailure { error ->
                            routeBridgeError(error.message ?: "WalletConnect proposal handling failed")
                        }
                    }
                }

                override fun onSessionRequest(request: WalletAbiWalletConnectSessionRequest) {
                    applicationScope.launch(Dispatchers.IO) {
                        val requestKey = request.requestKey()
                        val accepted = bridgeRequestMutex.withLock {
                            inFlightBridgeRequests.add(requestKey)
                        }
                        if (!accepted) {
                            logger.w {
                                "dropping concurrent duplicate walletconnect request " +
                                    "topic=${request.topic} requestId=${request.requestId} method=${request.method}"
                            }
                            return@launch
                        }
                        try {
                            runCatching {
                                handleSessionRequest(request)
                            }.onFailure { error ->
                                routeBridgeError(error.message ?: "WalletConnect request handling failed")
                            }
                        } finally {
                            bridgeRequestMutex.withLock {
                                inFlightBridgeRequests.remove(requestKey)
                            }
                        }
                    }
                }

                override fun onSessionDelete(topic: String) {
                    applicationScope.launch(Dispatchers.IO) {
                        runCatching {
                            handleSessionDelete(topic)
                        }.onFailure { error ->
                            routeBridgeError(error.message ?: "WalletConnect session delete handling failed")
                        }
                    }
                }

                override fun onSessionExtend(session: WalletAbiWalletConnectSessionInfo) {
                    applicationScope.launch(Dispatchers.IO) {
                        runCatching {
                            handleSessionExtend(session)
                        }.onFailure { error ->
                            routeBridgeError(error.message ?: "WalletConnect session extend handling failed")
                        }
                    }
                }

                override fun onError(message: String) {
                    applicationScope.launch(Dispatchers.IO) {
                        routeBridgeError(message)
                    }
                }
            },
        )
    }

    override fun state(walletId: String): StateFlow<WalletAbiWalletConnectState> {
        return stateFlow(walletId).asStateFlow()
    }

    override suspend fun bind(greenWallet: GreenWallet, session: GdkSession) = withContext(Dispatchers.IO) {
        logger.i { "bind walletId=${greenWallet.id}" }
        bridge.initialize()
        val recoveryState = readRecoveryState()
        val walletId = greenWallet.id
        val bindingTarget = resolveBindingTarget(session)
        val existingRuntime = runtimeMutex.withLock {
            runtimes[walletId]
        }
        if (existingRuntime?.matches(bindingTarget) == true) {
            logger.i { "bind reusing runtime walletId=$walletId accountId=${bindingTarget.primaryAccount.id}" }
            return@withContext
        }
        val nextRuntime = try {
            createRuntime(
                greenWallet = greenWallet,
                session = session,
                bindingTarget = bindingTarget,
                recoveryState = recoveryState,
            )
        } catch (error: Throwable) {
            logger.w { "bind failed walletId=$walletId message=${error.message}" }
            updateState(walletId, state(walletId).value.copy(bridgeError = error.message ?: "WalletConnect is unavailable"))
            return@withContext
        }

        val previous = runtimeMutex.withLock {
            runtimes.put(walletId, nextRuntime)
        }
        previous?.close()

        nextRuntime.operationMutex.withLock {
            refreshState(nextRuntime, bridgeError = null)
            reconcile(nextRuntime)
            logger.i {
                "bind reconciled walletId=$walletId overlay=${nextRuntime.state.value.uiState.currentOverlay?.kind} " +
                    "sessions=${nextRuntime.state.value.uiState.activeSessions.size}"
            }
        }
    }

    override suspend fun pair(greenWallet: GreenWallet, session: GdkSession, input: String) = withContext(Dispatchers.IO) {
        logger.i { "pair requested walletId=${greenWallet.id} inputPrefix=${input.take(32)}" }
        val runtime = runtimeMutex.withLock {
            runtimes[greenWallet.id]
        } ?: run {
            bind(greenWallet, session)
            runtimeMutex.withLock { runtimes[greenWallet.id] }
        } ?: return@withContext

        val hasPendingConnectionApproval = runtime.operationMutex.withLock {
            runtime.state.value.uiState.currentOverlay?.kind == WalletAbiWalletConnectOverlayKind.CONNECTION_APPROVAL
        }
        if (hasPendingConnectionApproval) {
            logger.i { "pair ignored walletId=${greenWallet.id} reason=pending_connection_approval" }
            return@withContext
        }

        val pairReserved = runtimeMutex.withLock {
            if (pendingPairWalletId == greenWallet.id) {
                false
            } else {
                pendingPairWalletId = greenWallet.id
                true
            }
        }
        if (!pairReserved) {
            logger.i { "pair ignored walletId=${greenWallet.id} reason=pair_already_in_flight" }
            return@withContext
        }

        val normalized = runtime.operationMutex.withLock {
            runtime.coordinator.clearError()
            refreshState(runtime, bridgeError = null)
            runtime.coordinator.normalizePairingUri(input)
        }
        updateState(
            greenWallet.id,
            state(greenWallet.id).value.copy(
                bridgeError = null,
                isPairing = true,
            ),
        )

        runCatching {
            bridge.pair(normalized)
            logger.i { "pair dispatched walletId=${greenWallet.id}" }
        }.onFailure { error ->
            runtimeMutex.withLock {
                if (pendingPairWalletId == greenWallet.id) {
                    pendingPairWalletId = null
                }
            }
            logger.w { "pair failed walletId=${greenWallet.id} message=${error.message}" }
            updateState(
                greenWallet.id,
                state(greenWallet.id).value.copy(
                    bridgeError = error.message ?: "WalletConnect pairing failed",
                    isPairing = false,
                ),
            )
            throw error
        }
    }

    override suspend fun approveCurrentOverlay(walletId: String): WalletAbiWalletConnectActionOutcome? = withContext(Dispatchers.IO) {
        val runtime = runtimeMutex.withLock { runtimes[walletId] } ?: return@withContext null
        runtime.operationMutex.withLock {
            val actions = runtime.coordinator.approveCurrentOverlay()
            refreshState(runtime, bridgeError = null)
            executeActions(runtime, actions)
        }
    }

    override suspend fun rejectCurrentOverlay(walletId: String): WalletAbiWalletConnectActionOutcome? = withContext(Dispatchers.IO) {
        val runtime = runtimeMutex.withLock { runtimes[walletId] } ?: return@withContext null
        runtime.operationMutex.withLock {
            val actions = runtime.coordinator.rejectCurrentOverlay()
            refreshState(runtime, bridgeError = null)
            executeActions(runtime, actions)
        }
    }

    override suspend fun disconnectActiveSession(walletId: String): WalletAbiWalletConnectActionOutcome? = withContext(Dispatchers.IO) {
        val runtime = runtimeMutex.withLock { runtimes[walletId] } ?: return@withContext null
        runtime.operationMutex.withLock {
            val actions = runtime.coordinator.disconnectActiveSession(runtime.bundle.chainId)
            refreshState(runtime, bridgeError = null)
            executeActions(runtime, actions)
        }
    }

    override suspend fun clearError(walletId: String) {
        withContext(Dispatchers.IO) {
            runtimeMutex.withLock { runtimes[walletId] }?.also { runtime ->
                runtime.operationMutex.withLock {
                    runtime.coordinator.clearError()
                    val current = state(walletId).value
                    updateState(walletId, current.copy(bridgeError = null, uiState = current.uiState.copy(lastError = null)))
                    refreshState(runtime, bridgeError = null)
                }
            }
        }
    }

    private suspend fun handleSessionProposal(proposal: WalletAbiWalletConnectSessionProposal) {
        logger.i {
            "handleSessionProposal proposalId=${proposal.proposalId} required=${proposal.requiredMethods.joinToString()} " +
                "optional=${proposal.optionalMethods.joinToString()}"
        }
        val walletId = runtimeMutex.withLock {
            val pending = pendingPairWalletId
            pendingPairWalletId = null
            pending
        } ?: runtimeMutex.withLock {
            runtimes.keys.singleOrNull()
        } ?: return

        val runtime = runtimeMutex.withLock { runtimes[walletId] } ?: return
        runtime.operationMutex.withLock {
            if (shouldRejectDuplicatePendingProposal(runtime, proposal)) {
                logger.w {
                    "rejecting duplicate pending walletconnect proposal walletId=$walletId proposalId=${proposal.proposalId} " +
                        "requestedChains=${proposal.requestedChainIds().joinToString()}"
                }
                bridge.rejectProposal(
                    proposalId = proposal.proposalId,
                    message = "WalletConnect session approval is already pending for this wallet",
                )
                return
            }
            val actions = runtime.coordinator.handleSessionProposal(proposal)
            refreshState(runtime, bridgeError = null)
            logger.i {
                "proposal mapped walletId=$walletId overlay=${runtime.state.value.uiState.currentOverlay?.kind} actions=${actions.size}"
            }
            executeActions(runtime, actions)
        }
    }

    private suspend fun handleSessionRequest(request: WalletAbiWalletConnectSessionRequest) {
        logger.i {
            "handleSessionRequest topic=${request.topic} requestId=${request.requestId} method=${request.method}"
        }
        val runtime = runtimeForTopic(request.topic) ?: return
        runtime.operationMutex.withLock {
            if (shouldRejectConcurrentProcessRequest(runtime, request)) {
                logger.w {
                    "rejecting overlapping walletconnect tx request walletId=${runtime.walletId} " +
                        "topic=${request.topic} requestId=${request.requestId}"
                }
                bridge.respondWalletAbiError(
                    topic = request.topic,
                    requestId = request.requestId,
                    message = "Another Wallet ABI request is already pending approval",
                    errorKind = WalletAbiWalletConnectRpcErrorKind.INTERNAL_ERROR,
                )
                return@withLock
            }
            if (request.method == WALLET_ABI_PROCESS_REQUEST_METHOD) {
                updateState(
                    runtime.walletId,
                    runtime.state.value.copy(
                        bridgeError = null,
                        preparingRequest = WalletAbiWalletConnectPreparingRequest(
                            topic = request.topic,
                            requestId = request.requestId.toString(),
                            method = request.method,
                        ),
                    ),
                )
            }
            val actions = runtime.coordinator.handleSessionRequest(request)
            refreshState(runtime, bridgeError = null)
            logger.i {
                "request mapped walletId=${runtime.walletId} overlay=${runtime.state.value.uiState.currentOverlay?.kind} actions=${actions.size}"
            }
            executeActions(runtime, actions)
        }
    }

    private suspend fun handleSessionDelete(topic: String) {
        val runtime = runtimeForTopic(topic) ?: return
        runtime.operationMutex.withLock {
            runtime.coordinator.handleSessionDelete(topic)
            refreshState(runtime, bridgeError = null)
        }
    }

    private suspend fun handleSessionExtend(sessionInfo: WalletAbiWalletConnectSessionInfo) {
        val runtime = runtimeForTopic(sessionInfo.topic) ?: return
        runtime.operationMutex.withLock {
            runtime.coordinator.handleSessionExtend(sessionInfo)
            refreshState(runtime, bridgeError = null)
        }
    }

    private suspend fun routeBridgeError(message: String) {
        logger.w { "routeBridgeError message=$message" }
        val targetWalletId = runtimeMutex.withLock {
            pendingPairWalletId
                ?: runtimes.entries.firstOrNull { (_, runtime) ->
                    runtime.state.value.uiState.pendingActionCount > 0u ||
                        runtime.state.value.uiState.currentOverlay != null ||
                        runtime.state.value.uiState.activeSessions.any()
                }?.key
                ?: runtimes.keys.firstOrNull()
        } ?: return

        updateState(
            targetWalletId,
            state(targetWalletId).value.copy(
                bridgeError = message,
                isPairing = false,
                preparingRequest = null,
            ),
        )
    }

    private suspend fun reconcile(runtime: WalletRuntime) {
        val activeSessions = bridge.getActiveSessions()
        val activeSessionActions = runtime.coordinator.reconcileActiveSessions(activeSessions)
        refreshState(runtime, bridgeError = null)
        executeActions(runtime, activeSessionActions)

        val pendingRequests = bridge.getActiveSessions()
            .flatMap { sessionInfo -> bridge.getPendingRequests(sessionInfo.topic) }
        val reconcileResult = runtime.coordinator.reconcilePendingRequests(pendingRequests)
        refreshState(runtime, bridgeError = null)
        executeActions(runtime, reconcileResult.actions)
        reconcileResult.requestsToReplay.forEach { request ->
            val replayActions = runCatching {
                runtime.coordinator.handleSessionRequest(request)
            }.getOrElse {
                // A replay-only request that can no longer be restored should not poison
                // the whole wallet runtime. Drop the stale session and allow fresh pairing.
                val disconnectActions = runtime.coordinator.disconnectActiveSession(runtime.bundle.chainId)
                refreshState(runtime, bridgeError = null)
                executeActions(runtime, disconnectActions)
                return@forEach
            }

            refreshState(runtime, bridgeError = null)
            executeActions(runtime, replayActions)
        }
    }

    private suspend fun executeActions(
        runtime: WalletRuntime,
        actions: List<WalletAbiWalletConnectTransportAction>,
    ): WalletAbiWalletConnectActionOutcome? {
        var lastOutcome: WalletAbiWalletConnectActionOutcome? = null

        actions.forEach { action ->
            try {
                logger.i {
                    "executeAction walletId=${runtime.walletId} kind=${action.kind} actionId=${action.actionId}"
                }
                val execution = bridge.execute(action)
                if (action.kind == WalletAbiWalletConnectTransportActionKind.APPROVE_SESSION) {
                    val confirmedSessionInfo = execution.confirmedSessionInfo
                        ?: throw IllegalStateException("WalletConnect session approval did not return session info")
                    runtime.coordinator.handleApproveSessionSucceeded(
                        actionId = execution.actionId,
                        confirmedSessionInfo = confirmedSessionInfo,
                    )
                } else {
                    runtime.coordinator.handleTransportActionSucceeded(execution.actionId)
                }

                lastOutcome = action.toActionOutcome()
                logger.i {
                    "executeActionSucceeded walletId=${runtime.walletId} kind=${action.kind} actionId=${action.actionId} " +
                        "confirmedTopic=${execution.confirmedSessionInfo?.topic}"
                }
                refreshState(runtime, bridgeError = null)
                if (action.kind == WalletAbiWalletConnectTransportActionKind.APPROVE_SESSION) {
                    reconcile(runtime)
                }
            } catch (error: Throwable) {
                logger.w {
                    "executeActionFailed walletId=${runtime.walletId} kind=${action.kind} actionId=${action.actionId} " +
                        "message=${error.message}"
                }
                runCatching {
                    runtime.coordinator.handleTransportActionFailed(
                        action.actionId,
                        error.message ?: "WalletConnect transport action failed",
                    )
                }
                refreshState(
                    runtime,
                    bridgeError = error.message ?: "WalletConnect transport action failed",
                )
                return lastOutcome
            }
        }

        return lastOutcome
    }

    private suspend fun runtimeForTopic(topic: String): WalletRuntime? {
        return runtimeMutex.withLock {
            runtimes.values.firstOrNull { runtime ->
                runtime.state.value.uiState.activeSessions.any { it.topic == topic }
            }
        }
    }

    private suspend fun refreshState(
        runtime: WalletRuntime,
        bridgeError: String?,
    ) {
        val uiState = runtime.coordinator.uiState()
        val nextState = WalletAbiWalletConnectState(
            uiState = uiState,
            bridgeError = bridgeError,
            isPairing = false,
            preparingRequest = null,
        )
        updateState(runtime.walletId, nextState)
        persistSnapshot(runtime, nextState)
    }

    private suspend fun persistSnapshot(
        runtime: WalletRuntime,
        state: WalletAbiWalletConnectState,
    ) {
        if (state.uiState.activeSessions.isEmpty() &&
            state.uiState.currentOverlay == null &&
            state.uiState.pendingActionCount == 0u &&
            state.uiState.queuedOverlayCount == 0u
        ) {
            snapshotStore.clear(runtime.walletId)
            return
        }

        snapshotStore.save(
            walletId = runtime.walletId,
            snapshotJson = runtime.coordinator.snapshotJson(),
        )
    }

    private suspend fun createRuntime(
        greenWallet: GreenWallet,
        session: GdkSession,
        bindingTarget: WalletAbiBindingTarget,
        recoveryState: WalletAbiWalletConnectRecoveryState,
    ): WalletRuntime {
        val primaryAccount = bindingTarget.primaryAccount
        val snapshotAccounts = session.accounts.value.filter { account ->
            account.network.id == primaryAccount.network.id && !account.isLightning
        }
        val bundle = WalletAbiProviderBundle.create(
            walletId = greenWallet.id,
            session = session,
            primaryAccount = primaryAccount,
            snapshotAccounts = snapshotAccounts,
            esploraHttpClient = esploraHttpClient,
        )
        val snapshotJson = snapshotStore.load(greenWallet.id)
        val coordinator = if (snapshotJson.isNullOrBlank()) {
            WalletAbiWalletConnectCoordinator(bundle.provider, greenWallet.id)
        } else {
            runCatching {
                WalletAbiWalletConnectCoordinator.fromSnapshotJson(
                    provider = bundle.provider,
                    walletId = greenWallet.id,
                    snapshotJson = snapshotJson,
                )
            }.getOrElse {
                snapshotStore.clear(greenWallet.id)
                WalletAbiWalletConnectCoordinator(bundle.provider, greenWallet.id)
            }.let { restored ->
                val overlay = restored.uiState().currentOverlay
                val shouldDropRestoredOverlay = when (overlay?.kind) {
                    WalletAbiWalletConnectOverlayKind.CONNECTION_APPROVAL -> true
                    WalletAbiWalletConnectOverlayKind.TRANSACTION_APPROVAL -> {
                        val topic = overlay.sessionTopic
                        val requestId = overlay.request?.requestId
                        recoveryState.pendingRequests.none { pendingRequest ->
                            pendingRequest.topic == topic && pendingRequest.requestId == requestId
                        }
                    }

                    null -> false
                }
                if (shouldDropRestoredOverlay) {
                    logger.w {
                        "dropping stale walletconnect overlay snapshot walletId=${greenWallet.id} " +
                            "kind=${overlay?.kind} topic=${overlay?.sessionTopic} requestId=${overlay?.request?.requestId}"
                    }
                    snapshotStore.clear(greenWallet.id)
                    restored.close()
                    WalletAbiWalletConnectCoordinator(bundle.provider, greenWallet.id)
                } else {
                    restored
                }
            }
        }
        return WalletRuntime(
            walletId = greenWallet.id,
            bundle = bundle,
            coordinator = coordinator,
            state = stateFlow(greenWallet.id),
            accountId = primaryAccount.id,
        )
    }

    private fun updateState(walletId: String, nextState: WalletAbiWalletConnectState) {
        stateFlow(walletId).value = nextState
    }

    private fun shouldRejectDuplicatePendingProposal(
        runtime: WalletRuntime,
        proposal: WalletAbiWalletConnectSessionProposal,
    ): Boolean {
        val requestedChainIds = proposal.requestedChainIds()
            .ifEmpty { setOf(runtime.bundle.chainId) }

        runtime.state.value.uiState.currentOverlay
            ?.takeIf { it.kind == WalletAbiWalletConnectOverlayKind.CONNECTION_APPROVAL }
            ?.let { pendingOverlay ->
                val pendingChainId = pendingOverlay.chainId.takeIf { it.isNotBlank() }
                if (pendingChainId == null || pendingChainId in requestedChainIds) {
                    val pendingProposal = pendingOverlay.proposal
                    if (pendingProposal?.isSamePeer(proposal) != false) {
                        return true
                    }
                }
            }

        runtime.state.value.uiState.activeSessions.any { sessionInfo ->
            sessionInfo.matchesProposal(proposal, requestedChainIds)
        }.also { hasMatchingActiveSession ->
            if (hasMatchingActiveSession) {
                return true
            }
        }
        return false
    }

    private fun shouldRejectConcurrentProcessRequest(
        runtime: WalletRuntime,
        request: WalletAbiWalletConnectSessionRequest,
    ): Boolean {
        if (request.method != WALLET_ABI_PROCESS_REQUEST_METHOD) {
            return false
        }

        val currentOverlay = runtime.state.value.uiState.currentOverlay
            ?.takeIf { it.kind == WalletAbiWalletConnectOverlayKind.TRANSACTION_APPROVAL }
            ?: return false

        return currentOverlay.sessionTopic == request.topic
    }

    private fun stateFlow(walletId: String): MutableStateFlow<WalletAbiWalletConnectState> {
        return synchronized(stateFlows) {
            stateFlows.getOrPut(walletId) {
                MutableStateFlow(WalletAbiWalletConnectState())
            }
        }
    }

    private suspend fun readRecoveryState(): WalletAbiWalletConnectRecoveryState {
        val activeSessions = bridge.getActiveSessions()
        val pendingRequests = activeSessions.flatMap { sessionInfo ->
            bridge.getPendingRequests(sessionInfo.topic)
        }
        return WalletAbiWalletConnectRecoveryState(
            activeSessions = activeSessions,
            pendingRequests = pendingRequests,
        )
    }
}

private data class WalletRuntime(
    val walletId: String,
    val bundle: WalletAbiProviderBundle,
    val coordinator: WalletAbiWalletConnectCoordinator,
    val state: MutableStateFlow<WalletAbiWalletConnectState>,
    val accountId: String,
    val operationMutex: Mutex = Mutex(),
) {
    fun matches(target: WalletAbiBindingTarget): Boolean {
        return accountId == target.primaryAccount.id &&
            bundle.chainId == target.primaryAccount.network.walletAbiWalletConnectChainId()
    }

    fun close() {
        bundle.close()
    }
}

private data class WalletAbiBindingTarget(
    val primaryAccount: Account,
)

private data class WalletAbiWalletConnectRecoveryState(
    val activeSessions: List<WalletAbiWalletConnectSessionInfo>,
    val pendingRequests: List<WalletAbiWalletConnectSessionRequest>,
)

private data class WalletAbiWalletConnectRequestKey(
    val topic: String,
    val requestId: String,
    val method: String,
)

private const val WALLET_ABI_PROCESS_REQUEST_METHOD = "wallet_abi_process_request"

private fun WalletAbiWalletConnectSessionRequest.requestKey(): WalletAbiWalletConnectRequestKey {
    return WalletAbiWalletConnectRequestKey(
        topic = topic,
        requestId = requestId.toString(),
        method = method,
    )
}

private data class WalletAbiProviderBundle(
    val provider: WalletAbiProvider,
    val signerLink: SignerMetaLink,
    val signer: Signer,
    val mnemonic: Mnemonic,
    val sessionFactoryLink: WalletSessionFactoryLink,
    val outputAllocatorLink: WalletOutputAllocatorLink,
    val prevoutResolverLink: WalletPrevoutResolverLink,
    val broadcasterLink: WalletBroadcasterLink,
    val receiveAddressProviderLink: WalletReceiveAddressProviderLink,
    val runtimeDepsLink: WalletRuntimeDepsLink,
    val chainId: String,
) {
    fun close() {
        provider.close()
        runtimeDepsLink.close()
        receiveAddressProviderLink.close()
        broadcasterLink.close()
        prevoutResolverLink.close()
        outputAllocatorLink.close()
        sessionFactoryLink.close()
        signerLink.close()
        signer.close()
        mnemonic.close()
    }

    companion object {
        suspend fun create(
            walletId: String,
            session: GdkSession,
            primaryAccount: Account,
            snapshotAccounts: List<Account>,
            esploraHttpClient: WalletAbiEsploraHttpClient,
        ): WalletAbiProviderBundle {
            val mnemonicString = session.getCredentials().mnemonic
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("WalletConnect Wallet ABI requires mnemonic credentials")
            val lwkNetwork = primaryAccount.network.toLwkNetwork()
            val mnemonic = Mnemonic(mnemonicString)
            val signer = Signer(mnemonic, lwkNetwork)
            val signerLink = SignerMetaLink.fromSoftwareSigner(
                signer = signer,
                context = WalletAbiSignerContext(
                    network = lwkNetwork,
                    accountIndex = primaryAccount.walletAbiAccountIndex(),
                ),
            )
            val snapshotSupport = WalletAbiWalletSnapshotSupport(
                session = session,
                primaryAccount = primaryAccount,
                snapshotAccounts = snapshotAccounts,
                lwkNetwork = lwkNetwork,
                esploraHttpClient = esploraHttpClient,
            )
            val sessionFactoryLink = WalletSessionFactoryLink(snapshotSupport)
            val outputAllocatorLink = WalletOutputAllocatorLink(snapshotSupport)
            val prevoutResolverLink = WalletPrevoutResolverLink(snapshotSupport)
            val broadcasterLink = WalletBroadcasterLink(snapshotSupport)
            val receiveAddressProviderLink = WalletReceiveAddressProviderLink(snapshotSupport)
            val runtimeDepsLink = WalletRuntimeDepsLink(
                sessionFactory = sessionFactoryLink,
                outputAllocator = outputAllocatorLink,
                prevoutResolver = prevoutResolverLink,
                broadcaster = broadcasterLink,
                receiveAddressProvider = receiveAddressProviderLink,
            )
            val provider = WalletAbiProvider(
                signer = signerLink,
                wallet = runtimeDepsLink,
            )
            return WalletAbiProviderBundle(
                provider = provider,
                signerLink = signerLink,
                signer = signer,
                mnemonic = mnemonic,
                sessionFactoryLink = sessionFactoryLink,
                outputAllocatorLink = outputAllocatorLink,
                prevoutResolverLink = prevoutResolverLink,
                broadcasterLink = broadcasterLink,
                receiveAddressProviderLink = receiveAddressProviderLink,
                runtimeDepsLink = runtimeDepsLink,
                chainId = primaryAccount.network.walletAbiWalletConnectChainId(),
            )
        }
    }
}

private fun WalletAbiWalletConnectTransportAction.toActionOutcome(): WalletAbiWalletConnectActionOutcome? {
    return when (kind) {
        WalletAbiWalletConnectTransportActionKind.APPROVE_SESSION -> WalletAbiWalletConnectActionOutcome.SessionApproved
        WalletAbiWalletConnectTransportActionKind.REJECT_SESSION -> WalletAbiWalletConnectActionOutcome.SessionRejected
        WalletAbiWalletConnectTransportActionKind.RESPOND_SUCCESS ->
            respondSuccess?.resultJson?.let(WalletAbiWalletConnectActionOutcome::RequestSucceeded)
        WalletAbiWalletConnectTransportActionKind.RESPOND_WALLET_ABI_ERROR ->
            respondWalletAbiError?.message?.let(WalletAbiWalletConnectActionOutcome::RequestRejected)
        WalletAbiWalletConnectTransportActionKind.DISCONNECT_SESSION -> WalletAbiWalletConnectActionOutcome.SessionDisconnected
    }
}

private fun isSupportedWalletAbiAccount(account: Account): Boolean {
    return account.isLiquid && !account.isLightning && !account.isMultisig
}

private fun resolveBindingTarget(session: GdkSession): WalletAbiBindingTarget {
    val primaryAccount = session.activeAccount.value
        ?.takeIf(::isSupportedWalletAbiAccount)
        ?: session.accounts.value.firstOrNull(::isSupportedWalletAbiAccount)
        ?: throw IllegalStateException("WalletConnect Wallet ABI requires an active Liquid singlesig account")
    return WalletAbiBindingTarget(primaryAccount = primaryAccount)
}

private fun Network.toLwkNetwork(): LwkNetwork {
    return when {
        isLiquidMainnet -> LwkNetwork.mainnet()
        isLiquidTestnet && !isDevelopment -> LwkNetwork.testnet()
        isLiquid && isDevelopment -> LwkNetwork.regtestDefault()
        else -> throw IllegalStateException("WalletConnect Wallet ABI supports Liquid networks only")
    }
}

private fun Network.walletAbiWalletConnectChainId(): String {
    return when {
        isLiquidMainnet -> "walabi:liquid"
        isLiquidTestnet && !isDevelopment -> "walabi:testnet-liquid"
        isLiquid && isDevelopment -> "walabi:localtest-liquid"
        else -> throw IllegalStateException("WalletConnect Wallet ABI supports Liquid networks only")
    }
}

private fun Account.walletAbiAccountIndex(): UInt {
    derivationPath
        ?.lastOrNull()
        ?.let { child ->
            val hardenedBit = 0x80000000L
            val normalized = if (child >= hardenedBit) child - hardenedBit else child
            if (normalized in 0..UInt.MAX_VALUE.toLong()) {
                return normalized.toUInt()
            }
        }

    val fallbackIndex = if (type.isSinglesig()) {
        pointer / 16L
    } else {
        pointer
    }.coerceAtLeast(0L)

    return fallbackIndex.coerceAtMost(UInt.MAX_VALUE.toLong()).toUInt()
}

private fun WalletAbiWalletConnectSessionProposal.requestedChainIds(): Set<String> {
    return buildSet {
        addAll(requiredChainIds.filter { it.isNotBlank() })
        addAll(optionalChainIds.filter { it.isNotBlank() })
    }
}

private fun WalletAbiWalletConnectSessionProposal.isSamePeer(
    other: WalletAbiWalletConnectSessionProposal,
): Boolean {
    return name == other.name &&
        url == other.url &&
        description == other.description &&
        requestedChainIds() == other.requestedChainIds() &&
        requiredMethods.toSet() == other.requiredMethods.toSet() &&
        optionalMethods.toSet() == other.optionalMethods.toSet()
}

private fun WalletAbiWalletConnectSessionInfo.matchesProposal(
    proposal: WalletAbiWalletConnectSessionProposal,
    requestedChainIds: Set<String>,
): Boolean {
    if (peerName != proposal.name || peerUrl != proposal.url) {
        return false
    }

    return chainId in requestedChainIds
}

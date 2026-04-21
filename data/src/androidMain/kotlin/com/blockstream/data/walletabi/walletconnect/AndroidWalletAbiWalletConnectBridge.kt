package com.blockstream.data.walletabi.walletconnect

import android.app.Application
import com.blockstream.data.data.AppConfig
import com.blockstream.utils.Loggable
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lwk.WalletAbiWalletConnectApproveSessionAction
import lwk.WalletAbiWalletConnectDisconnectSessionAction
import lwk.WalletAbiWalletConnectReasonKind
import lwk.WalletAbiWalletConnectRespondSuccessAction
import lwk.WalletAbiWalletConnectRespondWalletAbiErrorAction
import lwk.WalletAbiWalletConnectRpcErrorKind
import lwk.WalletAbiWalletConnectSessionInfo
import lwk.WalletAbiWalletConnectSessionProposal
import lwk.WalletAbiWalletConnectSessionRequest
import lwk.WalletAbiWalletConnectTransportAction
import lwk.WalletAbiWalletConnectTransportActionKind
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val GET_SIGNER_RECEIVE_ADDRESS_METHOD = "get_signer_receive_address"
private const val GET_RAW_SIGNING_X_ONLY_PUBKEY_METHOD = "get_raw_signing_x_only_pubkey"
private const val WALLET_ABI_GET_CAPABILITIES_METHOD = "wallet_abi_get_capabilities"

class AndroidWalletAbiWalletConnectBridge(
    private val application: Application,
    private val appConfig: AppConfig,
) : Loggable(), WalletAbiWalletConnectBridge {
    private val initializationMutex = Mutex()
    private val initializationStateLock = Any()
    private val sessionProposals = linkedMapOf<Long, Wallet.Model.SessionProposal>()

    @Volatile
    private var initialized = false

    @Volatile
    private var initializationDeferred: CompletableDeferred<Unit>? = null

    private var listener: WalletAbiWalletConnectBridgeListener? = null

    private companion object {
        const val SESSION_APPROVAL_LOOKUP_ATTEMPTS = 120
        const val SESSION_APPROVAL_LOOKUP_DELAY_MS = 250L
    }

    override suspend fun initialize() {
        if (initialized) {
            return
        }
        logger.i { "initialize requested" }

        val deferred = initializationMutex.withLock {
            if (initialized) {
                return@withLock null
            }

            initializationDeferred?.let { existing ->
                return@withLock existing
            }

            val next = CompletableDeferred<Unit>()
            initializationDeferred = next

            try {
                startInitialization(next)
            } catch (error: Throwable) {
                synchronized(initializationStateLock) {
                    if (initializationDeferred === next) {
                        initializationDeferred = null
                        initialized = false
                    }
                }
                next.completeExceptionally(error)
                throw error
            }

            next
        }

        deferred?.await()
    }

    override fun setListener(listener: WalletAbiWalletConnectBridgeListener?) {
        this.listener = listener
    }

    override suspend fun pair(uri: String) {
        initialize()
        logger.i { "pair uriPrefix=${uri.take(48)}" }
        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = {
                    logger.i { "WalletKit.pair success" }
                    onSuccess(Unit)
                },
                onError = { error -> onError(error.throwable) },
            )
        }
    }

    override suspend fun rejectProposal(proposalId: ULong, message: String) {
        initialize()
        val proposal = takeSessionProposal(proposalId.toLong())
            ?: error("Missing WalletConnect proposal $proposalId")

        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.rejectSession(
                params = Wallet.Params.SessionReject(
                    proposerPublicKey = proposal.proposerPublicKey,
                    reason = message,
                ),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }
    }

    override suspend fun respondWalletAbiError(
        topic: String,
        requestId: ULong,
        message: String,
        errorKind: WalletAbiWalletConnectRpcErrorKind,
    ) {
        initialize()
        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.respondSessionRequest(
                params = Wallet.Params.SessionRequestResponse(
                    sessionTopic = topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = requestId.toLong(),
                        code = errorKind.toRpcCode(),
                        message = message,
                    ),
                ),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }
    }

    override suspend fun getActiveSessions(): List<WalletAbiWalletConnectSessionInfo> {
        initialize()
        return WalletKit.getListOfActiveSessions().map(Wallet.Model.Session::toLwk)
    }

    override suspend fun getPendingRequests(topic: String): List<WalletAbiWalletConnectSessionRequest> {
        initialize()
        return WalletKit.getPendingListOfSessionRequests(topic).map(Wallet.Model.SessionRequest::toLwk)
    }

    override suspend fun execute(
        action: WalletAbiWalletConnectTransportAction,
    ): WalletAbiWalletConnectTransportExecutionResult {
        initialize()

        return when (action.kind) {
            WalletAbiWalletConnectTransportActionKind.APPROVE_SESSION -> {
                val payload = action.approveSession
                    ?: error("WalletConnect approve session payload is missing")
                executeApproveSession(action.actionId, payload)
            }

            WalletAbiWalletConnectTransportActionKind.REJECT_SESSION -> {
                val payload = action.rejectSession
                    ?: error("WalletConnect reject session payload is missing")
                executeRejectSession(action.actionId, payload.proposalId.toLong(), payload.message)
            }

            WalletAbiWalletConnectTransportActionKind.RESPOND_SUCCESS -> {
                val payload = action.respondSuccess
                    ?: error("WalletConnect success response payload is missing")
                executeRespondSuccess(action.actionId, payload)
            }

            WalletAbiWalletConnectTransportActionKind.RESPOND_WALLET_ABI_ERROR -> {
                val payload = action.respondWalletAbiError
                    ?: error("WalletConnect error response payload is missing")
                executeRespondError(action.actionId, payload)
            }

            WalletAbiWalletConnectTransportActionKind.DISCONNECT_SESSION -> {
                val payload = action.disconnectSession
                    ?: error("WalletConnect disconnect payload is missing")
                executeDisconnect(action.actionId, payload)
            }
        }
    }

    private suspend fun executeApproveSession(
        actionId: String,
        payload: WalletAbiWalletConnectApproveSessionAction,
    ): WalletAbiWalletConnectTransportExecutionResult {
        logger.i {
            "executeApproveSession actionId=$actionId proposalId=${payload.proposalId} chainId=${payload.chainId} " +
                "methods=${payload.methods.joinToString()}"
        }
        val proposal = takeSessionProposal(payload.proposalId.toLong())
            ?: error("Missing WalletConnect proposal ${payload.proposalId}")
        val activeTopicsBefore = WalletKit.getListOfActiveSessions().mapTo(linkedSetOf()) { it.topic }

        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.approveSession(
                params = Wallet.Params.SessionApprove(
                    proposerPublicKey = proposal.proposerPublicKey,
                    namespaces = mapOf(
                        payload.chainId.substringBefore(':') to Wallet.Model.Namespace.Session(
                            chains = listOf(payload.chainId),
                            accounts = payload.accounts,
                            methods = payload.methods,
                            events = emptyList(),
                        ),
                    ),
                    relayProtocol = proposal.relayProtocol.takeIf { it.isNotBlank() },
                ),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }

        val confirmedSessionInfo = waitForConfirmedSessionInfo(
            activeTopicsBefore = activeTopicsBefore,
            payload = payload,
            proposal = proposal,
        )
        logger.i {
            "executeApproveSession result actionId=$actionId confirmedTopic=${confirmedSessionInfo?.topic}"
        }

        return WalletAbiWalletConnectTransportExecutionResult(
            actionId = actionId,
            confirmedSessionInfo = confirmedSessionInfo,
        )
    }

    private suspend fun waitForConfirmedSessionInfo(
        activeTopicsBefore: Set<String>,
        payload: WalletAbiWalletConnectApproveSessionAction,
        proposal: Wallet.Model.SessionProposal,
    ): WalletAbiWalletConnectSessionInfo? {
        repeat(SESSION_APPROVAL_LOOKUP_ATTEMPTS) {
            WalletKit.getListOfActiveSessions()
                .firstOrNull { session ->
                    session.topic !in activeTopicsBefore &&
                        session.namespaces.values.any { namespace ->
                            namespace.accounts.any { account ->
                                account.startsWith("${payload.chainId}:")
                            }
                        }
                }
                ?.toLwk()
                ?.let { confirmed ->
                    logger.i { "waitForConfirmedSessionInfo matched new topic=${confirmed.topic}" }
                    return confirmed
                }

            WalletKit.getListOfActiveSessions()
                .firstOrNull { session ->
                    session.metaData?.url == proposal.url &&
                        session.metaData?.name == proposal.name &&
                        session.namespaces.values.any { namespace ->
                            namespace.accounts.any { account ->
                                account.startsWith("${payload.chainId}:")
                            }
                        }
                }
                ?.toLwk()
                ?.let { confirmed ->
                    logger.i { "waitForConfirmedSessionInfo matched existing topic=${confirmed.topic}" }
                    return confirmed
                }

            delay(SESSION_APPROVAL_LOOKUP_DELAY_MS)
        }

        logger.w {
            "waitForConfirmedSessionInfo timed out proposalId=${proposal.proposerPublicKey.take(16)} activeTopics=${WalletKit.getListOfActiveSessions().map { it.topic }}"
        }
        return null
    }

    private suspend fun executeRejectSession(
        actionId: String,
        proposalId: Long,
        message: String,
    ): WalletAbiWalletConnectTransportExecutionResult {
        val proposal = takeSessionProposal(proposalId)
            ?: error("Missing WalletConnect proposal $proposalId")

        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.rejectSession(
                params = Wallet.Params.SessionReject(
                    proposerPublicKey = proposal.proposerPublicKey,
                    reason = message,
                ),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }

        return WalletAbiWalletConnectTransportExecutionResult(actionId = actionId)
    }

    private fun takeSessionProposal(proposalId: Long): Wallet.Model.SessionProposal? {
        return synchronized(sessionProposals) {
            sessionProposals.remove(proposalId)
                ?: sessionProposals.entries.singleOrNull()?.let { entry ->
                    sessionProposals.remove(entry.key)
                }
        }
    }

    private suspend fun executeRespondSuccess(
        actionId: String,
        payload: WalletAbiWalletConnectRespondSuccessAction,
    ): WalletAbiWalletConnectTransportExecutionResult {
        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.respondSessionRequest(
                params = Wallet.Params.SessionRequestResponse(
                    sessionTopic = payload.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                        id = payload.requestId.toLong(),
                        result = payload.resultJson,
                    ),
                ),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }

        return WalletAbiWalletConnectTransportExecutionResult(actionId = actionId)
    }

    private suspend fun executeRespondError(
        actionId: String,
        payload: WalletAbiWalletConnectRespondWalletAbiErrorAction,
    ): WalletAbiWalletConnectTransportExecutionResult {
        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.respondSessionRequest(
                params = Wallet.Params.SessionRequestResponse(
                    sessionTopic = payload.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = payload.requestId.toLong(),
                        code = payload.errorKind.toRpcCode(),
                        message = payload.message,
                    ),
                ),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }

        return WalletAbiWalletConnectTransportExecutionResult(actionId = actionId)
    }

    private suspend fun executeDisconnect(
        actionId: String,
        payload: WalletAbiWalletConnectDisconnectSessionAction,
    ): WalletAbiWalletConnectTransportExecutionResult {
        awaitWalletKit<Unit> { onSuccess, onError ->
            WalletKit.disconnectSession(
                params = Wallet.Params.SessionDisconnect(sessionTopic = payload.topic),
                onSuccess = { onSuccess(Unit) },
                onError = { error -> onError(error.throwable) },
            )
        }

        return WalletAbiWalletConnectTransportExecutionResult(actionId = actionId)
    }

    private fun startInitialization(initialization: CompletableDeferred<Unit>) {
        val projectId = appConfig.reownProjectId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Missing reown_project_id in app keys")

        val metadata = Core.Model.AppMetaData(
            name = "Green",
            description = "Blockstream Green Wallet",
            url = "https://blockstream.com/green/",
            icons = listOf("https://blockstream.com/green/favicon.ico"),
            redirect = null,
        )

        CoreClient.initialize(
            application = application,
            projectId = projectId,
            metaData = metadata,
        ) { error ->
            failInitialization(
                initialization = initialization,
                throwable = error.throwable,
                fallbackMessage = "WalletConnect core initialization failed",
            )
        }

        WalletKit.initialize(
            params = Wallet.Params.Init(CoreClient),
            onSuccess = {
                completeInitialization(initialization)
            },
        ) { error ->
            failInitialization(
                initialization = initialization,
                throwable = error.throwable,
                fallbackMessage = "WalletKit initialization failed",
            )
        }

        WalletKit.setWalletDelegate(
            object : WalletKit.WalletDelegate {
                override fun onSessionProposal(
                    sessionProposal: Wallet.Model.SessionProposal,
                    verifyContext: Wallet.Model.VerifyContext,
                ) {
                    logger.i {
                        "onSessionProposal verifyId=${verifyContext.id} name=${sessionProposal.name} url=${sessionProposal.url}"
                    }
                    synchronized(sessionProposals) {
                        sessionProposals[verifyContext.id] = sessionProposal
                    }
                    listener?.onSessionProposal(sessionProposal.toLwk(verifyContext))
                }

                override fun onSessionRequest(
                    sessionRequest: Wallet.Model.SessionRequest,
                    verifyContext: Wallet.Model.VerifyContext,
                ) {
                    logger.i {
                        "onSessionRequest topic=${sessionRequest.topic} requestId=${sessionRequest.request.id} method=${sessionRequest.request.method}"
                    }
                    listener?.onSessionRequest(sessionRequest.toLwk())
                }

                override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                    when (sessionDelete) {
                        is Wallet.Model.SessionDelete.Success -> {
                            listener?.onSessionDelete(sessionDelete.topic)
                        }

                        is Wallet.Model.SessionDelete.Error -> {
                            listener?.onError(
                                sessionDelete.error.message ?: "WalletConnect session disconnected",
                            )
                        }
                    }
                }

                override fun onSessionExtend(session: Wallet.Model.Session) {
                    listener?.onSessionExtend(session.toLwk())
                }

                override fun onSessionSettleResponse(
                    settleSessionResponse: Wallet.Model.SettledSessionResponse,
                ) = Unit

                override fun onSessionUpdateResponse(
                    sessionUpdateResponse: Wallet.Model.SessionUpdateResponse,
                ) = Unit

                override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                    if (state.isAvailable) {
                        return
                    }

                    val message = state.reason?.let { reason ->
                        when (reason) {
                            is Wallet.Model.ConnectionState.Reason.ConnectionClosed -> reason.message
                            is Wallet.Model.ConnectionState.Reason.ConnectionFailed -> {
                                reason.throwable.message ?: "WalletConnect connection failed"
                            }
                        }
                    } ?: "WalletConnect connection unavailable"

                    listener?.onError(message)
                }

                override fun onError(error: Wallet.Model.Error) {
                    logger.w { "WalletKit delegate error=${error.throwable.message}" }
                    listener?.onError(error.throwable.message ?: "WalletConnect wallet error")
                }
            },
        )
    }

    private fun completeInitialization(initialization: CompletableDeferred<Unit>) {
        synchronized(initializationStateLock) {
            if (initializationDeferred !== initialization || initialization.isCompleted) {
                return
            }

            initialized = true
            initialization.complete(Unit)
        }
    }

    private fun failInitialization(
        initialization: CompletableDeferred<Unit>,
        throwable: Throwable,
        fallbackMessage: String,
    ) {
        val message = throwable.message ?: fallbackMessage
        val shouldNotify = synchronized(initializationStateLock) {
            if (initializationDeferred !== initialization) {
                return@synchronized false
            }

            initialized = false
            initializationDeferred = null
            if (!initialization.isCompleted) {
                initialization.completeExceptionally(throwable)
            }
            true
        }

        if (shouldNotify) {
            logger.w { message }
            listener?.onError(message)
        }
    }
}

private suspend fun <T> awaitWalletKit(
    block: (onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) -> Unit,
): T {
    return suspendCancellableCoroutine { continuation ->
        block(
            { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            },
            { throwable ->
                if (continuation.isActive) {
                    continuation.resumeWithException(throwable)
                }
            },
        )
    }
}

private fun Wallet.Model.SessionProposal.toLwk(
    verifyContext: Wallet.Model.VerifyContext,
): WalletAbiWalletConnectSessionProposal {
    val requiredNamespace = requiredNamespaces["walabi"]
    val optionalNamespace = optionalNamespaces["walabi"]

    return WalletAbiWalletConnectSessionProposal(
        proposalId = verifyContext.id.toULong(),
        pairingUri = null,
        name = name,
        description = description.takeIf { it.isNotBlank() },
        url = url.takeIf { it.isNotBlank() },
        icons = icons.map { it.toString() },
        requiredChainIds = requiredNamespace?.chains.orEmpty(),
        optionalChainIds = optionalNamespace?.chains.orEmpty(),
        requiredMethods = requiredNamespace?.methods.orEmpty(),
        optionalMethods = optionalNamespace?.methods.orEmpty(),
        requiredEvents = requiredNamespace?.events.orEmpty(),
        optionalEvents = optionalNamespace?.events.orEmpty(),
    )
}

private fun Wallet.Model.Session.toLwk(): WalletAbiWalletConnectSessionInfo {
    val walabiNamespace = namespaces["walabi"]

    return WalletAbiWalletConnectSessionInfo(
        topic = topic,
        chainId = walabiNamespace?.accounts?.firstOrNull()?.substringBeforeLast(':')
            ?: walabiNamespace?.chains?.firstOrNull()
            ?: "",
        methods = walabiNamespace?.methods.orEmpty(),
        accounts = walabiNamespace?.accounts.orEmpty(),
        peerName = metaData?.name?.takeIf { it.isNotBlank() },
        peerDescription = metaData?.description?.takeIf { it.isNotBlank() },
        peerUrl = metaData?.url?.takeIf { it.isNotBlank() },
        peerIcons = metaData?.icons.orEmpty().map { it.toString() },
    )
}

private fun Wallet.Model.SessionRequest.toLwk(): WalletAbiWalletConnectSessionRequest {
    val normalizedParams = when (request.method) {
        GET_SIGNER_RECEIVE_ADDRESS_METHOD,
        GET_RAW_SIGNING_X_ONLY_PUBKEY_METHOD,
        WALLET_ABI_GET_CAPABILITIES_METHOD,
        -> request.params.takeUnless { it == "{}" } ?: "null"

        else -> request.params
    }

    return WalletAbiWalletConnectSessionRequest(
        topic = topic,
        requestId = request.id.toULong(),
        chainId = chainId ?: "",
        method = request.method,
        paramsJson = normalizedParams,
    )
}

private fun WalletAbiWalletConnectRpcErrorKind.toRpcCode(): Int {
    return when (this) {
        WalletAbiWalletConnectRpcErrorKind.INVALID_REQUEST -> -32600
        WalletAbiWalletConnectRpcErrorKind.METHOD_NOT_SUPPORTED -> -32601
        WalletAbiWalletConnectRpcErrorKind.UNAUTHORIZED -> 4100
        WalletAbiWalletConnectRpcErrorKind.SESSION_NOT_FOUND -> -32000
        WalletAbiWalletConnectRpcErrorKind.INTERNAL_ERROR -> -32603
    }
}

private fun WalletAbiWalletConnectReasonKind.defaultMessage(): String {
    return when (this) {
        WalletAbiWalletConnectReasonKind.USER_REJECTED -> "WalletConnect request rejected by user"
        WalletAbiWalletConnectReasonKind.USER_DISCONNECTED -> "WalletConnect session disconnected by user"
        WalletAbiWalletConnectReasonKind.UNSUPPORTED_PROPOSAL -> "WalletConnect proposal is not supported"
        WalletAbiWalletConnectReasonKind.REPLACED_SESSION -> "WalletConnect session replaced by a newer connection"
        WalletAbiWalletConnectReasonKind.SESSION_DELETED -> "WalletConnect session deleted by peer"
        WalletAbiWalletConnectReasonKind.INTERNAL_ERROR -> "WalletConnect internal error"
    }
}

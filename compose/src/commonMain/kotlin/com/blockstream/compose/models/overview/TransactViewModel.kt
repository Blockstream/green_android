package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_transact
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewTransactionLook
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.looks.transaction.TransactionLook
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.WalletAbiFlowLaunchMode
import com.blockstream.data.data.DataState
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.toTransaction
import com.blockstream.data.json.DefaultJson
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectManaging
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectState
import com.blockstream.domain.base.Result
import com.blockstream.domain.meld.GetPendingMeldTransactions
import com.blockstream.domain.swap.IsSwapAvailableUseCase
import com.blockstream.domain.swap.IsSwapsEnabledUseCase
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiResumeSnapshot
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lwk.WalletAbiWalletConnectOverlayKind
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

data class WalletAbiWalletConnectCardLook(
    val title: String,
    val subtitle: String?,
    val body: String,
    val statusLabel: String,
)

abstract class TransactViewModelAbstract(
    greenWallet: GreenWallet
) : WalletBalanceViewModel(greenWallet = greenWallet) {

    override fun screenName(): String = "TransactTab"

    private val isSwapsEnabledUseCase: IsSwapsEnabledUseCase by inject()

    abstract val isSwapAvailable: Boolean

    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>

    abstract val pendingWalletAbiResume: StateFlow<WalletAbiResumeSnapshot?>
    abstract val walletAbiWalletConnectCard: StateFlow<WalletAbiWalletConnectCardLook?>
    abstract val hasPendingWalletAbiWalletConnectRequest: StateFlow<Boolean>

    fun onBuy() {
        postEvent(NavigateDestinations.Buy(greenWallet = greenWallet))
    }

    fun onSend() {
        postEvent(NavigateDestinations.SendAddress(greenWallet = greenWallet))
    }

    fun onReceive() {
        postEvent(NavigateDestinations.ReceiveChooseAsset(greenWallet = greenWallet))
    }

    fun onSwap() {
        viewModelScope.launchSafe {
            if (isSwapsEnabledUseCase(greenWallet)) {
                postEvent(NavigateDestinations.Swap(greenWallet = greenWallet, accountAsset = accountAsset.value))
            } else {
                postEvent(NavigateDestinations.EnableJadeFeature(greenWallet = greenWallet, accountAsset = accountAsset.value))
            }
        }
    }

    fun openWalletAbiFlow() {
        postEvent(
            NavigateDestinations.WalletAbiFlow(
                greenWallet = greenWallet,
                launchMode = WalletAbiFlowLaunchMode.Demo
            )
        )
    }

    fun resumePendingWalletAbiFlow() {
        postEvent(
            NavigateDestinations.WalletAbiFlow(
                greenWallet = greenWallet,
                launchMode = WalletAbiFlowLaunchMode.Resume
            )
        )
    }

    fun openWalletAbiWalletConnect() {
        postEvent(NavigateDestinations.WalletAbiWalletConnect(greenWallet = greenWallet))
    }

    abstract fun handleWalletConnectInput(input: String)
}

class TransactViewModel(greenWallet: GreenWallet) : TransactViewModelAbstract(greenWallet = greenWallet) {

    private val isSwapAvailableUseCase: IsSwapAvailableUseCase by inject()
    private val getPendingMeldTransactions: GetPendingMeldTransactions by inject()
    private val snapshotRepository: WalletAbiFlowSnapshotRepository by inject()
    private var refreshJob: Job? = null

    override fun segmentation(): HashMap<String, Any> = countly.sessionSegmentation(session = session)

    override val isSwapAvailable: Boolean = session.ifConnected {
        isSwapAvailableUseCase(wallet = greenWallet, session = session)
    } ?: false

    override val pendingWalletAbiResume: StateFlow<WalletAbiResumeSnapshot?> =
        snapshotRepository.observe(greenWallet.id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val walletAbiWalletConnectState: StateFlow<WalletAbiWalletConnectState> =
        walletAbiWalletConnectManager.state(greenWallet.id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), WalletAbiWalletConnectState())

    override val walletAbiWalletConnectCard: StateFlow<WalletAbiWalletConnectCardLook?> =
        walletAbiWalletConnectState
            .map { it.toCardLook() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    override val hasPendingWalletAbiWalletConnectRequest: StateFlow<Boolean> =
        walletAbiWalletConnectState
            .map { state ->
                state.preparingRequest != null ||
                    state.uiState.currentOverlay?.kind == WalletAbiWalletConnectOverlayKind.TRANSACTION_APPROVAL
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    private val _meldTransactions: StateFlow<List<Transaction>> = greenWallet.xPubHashId.let {
        combine(
            getPendingMeldTransactions.observe(), session.accounts
        ) { result, accounts ->
            when (result) {
                is Result.Success -> {
                    val bitcoinAccount = accounts.firstOrNull { it.isBitcoin && !it.isLightning }
                    if (bitcoinAccount != null) {
                        result.data.mapNotNull { it.toTransaction(bitcoinAccount) }
                    } else {
                        emptyList()
                    }
                }

                else -> emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    }

    private val _transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.walletTransactions.filter { session.isConnected }, session.settings(), _meldTransactions
    ) { transactions, _, meldTransactions ->
        transactions.mapSuccess { gdkTransactions ->
            // A txHash can appear across multiple accounts, so keep all gdk transactions
            // and only add meld transactions that don't overlap.
            val uniqueHashes = gdkTransactions.mapTo(mutableSetOf()) { it.txHash }

            val allTransactions =
                (gdkTransactions + meldTransactions.filter { it.txHash !in uniqueHashes }).sortedByDescending { it.createdAtTs }

            allTransactions.map {
                TransactionLook.create(
                    transaction = it, session = session, disableHideAmounts = true
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    // Re-calculate if needed (hideAmount or denomination & exchange rate change)
    override val transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        hideAmounts, _transactions
    ) { hideAmounts, transactionsLooks ->
        if (transactionsLooks is DataState.Success && hideAmounts) {
            DataState.Success(transactionsLooks.data.map { it.asMasked })
        } else {
            transactionsLooks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    init {
        greenWalletFlow.filterNotNull().onEach {
            updateNavData(it)
        }.launchIn(this)

        viewModelScope.launch {
            updateNavData(greenWallet)
        }

        getPendingMeldTransactions()
        viewModelScope.launch {
            walletAbiWalletConnectManager.bind(
                greenWallet = greenWallet,
                session = session,
            )
        }

        sessionManager.pendingUri
            .filterNotNull()
            .debounce(50L)
            .onEach {
                sessionManager.consumePendingUri(it)?.also { pendingUri ->
                    handleWalletConnectInput(pendingUri)
                }
            }
            .launchIn(this)

        startPeriodicRefresh()

        bootstrap()
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                getPendingMeldTransactions()
            }
        }
    }

    private fun getPendingMeldTransactions() {
        greenWallet.xPubHashId.let { xPubHashId ->
            viewModelScope.launch {
                getPendingMeldTransactions(
                    GetPendingMeldTransactions.Params(
                        externalCustomerId = xPubHashId
                    )
                )
            }
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }

    override fun handleWalletConnectInput(input: String) {
        val pairingInput = input.trim()
        if (pairingInput.isBlank()) {
            return
        }

        postEvent(
            NavigateDestinations.WalletAbiWalletConnect(
                greenWallet = greenWallet,
                pairingUri = pairingInput,
            )
        )
    }

    private suspend fun updateNavData(greenWallet: GreenWallet) {
        _navData.value = NavData(
            title = getString(Res.string.id_transact),
            walletName = greenWallet.name,
            showBadge = !greenWallet.isRecoveryConfirmed,
            showBottomNavigation = true
        )
    }
}

class TransactViewModelPreview(
    val isEmpty: Boolean = false,
    pendingWalletAbiResume: WalletAbiResumeSnapshot? = null
) : TransactViewModelAbstract(greenWallet = previewWallet()) {

    override val isSwapAvailable: Boolean = true

    override val transactions: StateFlow<DataState<List<TransactionLook>>> = MutableStateFlow(
        DataState.Success(
            if (isEmpty) listOf() else listOf(
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
                previewTransactionLook(),
            )
        )
    )

    override val pendingWalletAbiResume: StateFlow<WalletAbiResumeSnapshot?> =
        MutableStateFlow(pendingWalletAbiResume)

    override val walletAbiWalletConnectCard: StateFlow<WalletAbiWalletConnectCardLook?> =
        MutableStateFlow(null)

    override val hasPendingWalletAbiWalletConnectRequest: StateFlow<Boolean> =
        MutableStateFlow(false)

    override fun handleWalletConnectInput(input: String) = Unit

    companion object : Loggable() {
        fun create(
            isEmpty: Boolean = false,
            pendingWalletAbiResume: WalletAbiResumeSnapshot? = null
        ) = TransactViewModelPreview(
            isEmpty = isEmpty,
            pendingWalletAbiResume = pendingWalletAbiResume
        )
    }
}

private fun WalletAbiWalletConnectState.toCardLook(): WalletAbiWalletConnectCardLook? {
    val overlay = uiState.currentOverlay
    if (overlay != null) {
        return when (overlay.kind) {
            WalletAbiWalletConnectOverlayKind.CONNECTION_APPROVAL -> {
                val proposal = overlay.proposal
                val requestedMethods = listOf(
                    proposal?.requiredMethods.orEmpty(),
                    proposal?.optionalMethods.orEmpty(),
                ).flatten().distinct()

                WalletAbiWalletConnectCardLook(
                    title = proposal?.name?.takeIf { it.isNotBlank() } ?: "WalletConnect session request",
                    subtitle = overlay.chainId,
                    body = buildString {
                        if (requestedMethods.isNotEmpty()) {
                            append("Review access to ")
                            append(requestedMethods.joinToString())
                            append(".")
                        } else {
                            append("Review the WalletConnect session request before pairing.")
                        }
                    },
                    statusLabel = if (overlay.awaitingTransport) "Connecting" else "Review",
                )
            }

            WalletAbiWalletConnectOverlayKind.TRANSACTION_APPROVAL -> {
                val session = uiState.activeSessions.firstOrNull { it.topic == overlay.sessionTopic }
                val request = parseWalletAbiTxCreateRequest(overlay.requestJson)
                val outputCount = request?.params?.outputs?.size ?: 1
                val title = if (outputCount > 1) {
                    "Wallet ABI split request"
                } else {
                    "Wallet ABI transfer request"
                }
                WalletAbiWalletConnectCardLook(
                    title = title,
                    subtitle = session?.peerName ?: overlay.request?.method,
                    body = buildString {
                        append("Request ")
                        append(request?.requestId ?: overlay.request?.requestId?.toString() ?: "pending")
                        append(" is ready for review")
                        if (session?.peerName?.isNotBlank() == true) {
                            append(" from ")
                            append(session.peerName)
                        }
                        append(".")
                    },
                    statusLabel = if (overlay.awaitingTransport) "Sending" else "Review",
                )
            }
        }
    }

    if (isPairing) {
        return WalletAbiWalletConnectCardLook(
            title = "WalletConnect pairing",
            subtitle = null,
            body = "Waiting for the paired app to send a session proposal.",
            statusLabel = "Pairing",
        )
    }

    preparingRequest?.let { preparingRequest ->
        val session = uiState.activeSessions.firstOrNull { it.topic == preparingRequest.topic }
        return WalletAbiWalletConnectCardLook(
            title = "Wallet ABI request",
            subtitle = session?.peerName ?: preparingRequest.method,
            body = buildString {
                append("Preparing Wallet ABI review")
                session?.peerName?.takeIf { it.isNotBlank() }?.let { peerName ->
                    append(" from ")
                    append(peerName)
                }
                append(".")
            },
            statusLabel = "Preparing",
        )
    }

    val activeSession = uiState.activeSessions.firstOrNull() ?: return null
    return WalletAbiWalletConnectCardLook(
        title = activeSession.peerName?.takeIf { it.isNotBlank() } ?: "WalletConnect connected",
        subtitle = activeSession.chainId,
        body = if (uiState.pendingActionCount > 0u) {
            "Waiting for WalletConnect transport confirmation."
        } else {
            "Ready to review Wallet ABI transfer and split requests."
        },
        statusLabel = if (uiState.pendingActionCount > 0u) "Syncing" else "Connected",
    )
}

private fun parseWalletAbiTxCreateRequest(requestJson: String?): WalletAbiTxCreateRequest? {
    val payload = requestJson ?: return null
    return runCatching {
        DefaultJson.decodeFromString(WalletAbiTxCreateRequest.serializer(), payload)
    }.getOrNull()
}

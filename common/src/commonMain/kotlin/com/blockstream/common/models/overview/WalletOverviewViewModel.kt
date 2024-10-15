package com.blockstream.common.models.overview

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.dots_three_vertical_bold
import blockstream_green.common.generated.resources.id_lightning_account
import breez_sdk.HealthCheckStatus
import com.blockstream.common.Urls
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.DataState
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.toggle
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.common.gdk.data.WalletEvents
import com.blockstream.common.looks.account.LightningInfoLook
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.AppReviewHelper
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class WalletOverviewViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "WalletOverview"

    @NativeCoroutinesState
    abstract val alerts: StateFlow<List<AlertType>>

    @NativeCoroutinesState
    abstract val balancePrimary: StateFlow<String?>

    @NativeCoroutinesState
    abstract val balanceSecondary: StateFlow<String?>

    @NativeCoroutinesState
    abstract val hideAmounts: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val assetsVisibility: StateFlow<Boolean?>

    @NativeCoroutinesState
    abstract val isWalletOnboarding: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val assetIcons: StateFlow<List<String>>

    @NativeCoroutinesState
    abstract val totalAssets: StateFlow<Int>

    @NativeCoroutinesState
    abstract val activeAccount: StateFlow<Account?>

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<AccountBalance>>

    @NativeCoroutinesState
    abstract val lightningInfo: StateFlow<LightningInfoLook?>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>

    @NativeCoroutinesState
    abstract val archivedAccounts: StateFlow<Int>

    open val isLightningShortcut: Boolean = false
}

class WalletOverviewViewModel(greenWallet: GreenWallet) :
    WalletOverviewViewModelAbstract(greenWallet = greenWallet) {
    override fun segmentation(): HashMap<String, Any> =
        countly.sessionSegmentation(session = session)
    
    private val _balancePrimary: MutableStateFlow<String?> = MutableStateFlow(null)
    override val balancePrimary: StateFlow<String?> = _balancePrimary.asStateFlow()

    private val _balanceSecondary: MutableStateFlow<String?> = MutableStateFlow(null)
    override val balanceSecondary: StateFlow<String?> = _balanceSecondary.asStateFlow()

    override val activeAccount: StateFlow<Account?> = session.activeAccount

    override val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), settingsManager.appSettings.hideAmounts)

    override val assetsVisibility: StateFlow<Boolean?> = session.walletAssets.map {
        when {
            it.size > 1 && session.walletHasHistory -> true
            (session.activeBitcoin != null && session.activeLiquid != null && it.isEmpty()) -> null
            else -> false
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val isWalletOnboarding: StateFlow<Boolean> = combine(session.zeroAccounts, session.failedNetworks) { zeroAccounts, failedNetworks ->
        zeroAccounts && failedNetworks.isEmpty() && !session.isWatchOnly
    }.filter { session.isConnected }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    override val assetIcons: StateFlow<List<String>> = session.walletAssets
        .filter { session.isConnected }.map {
            it.assets.keys.map {
                if (it.isPolicyAsset(session) || session.hasAssetIcon(it)) {
                    it
                } else {
                    "unknown"
                }
            }.distinct()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val totalAssets: StateFlow<Int> = session.walletAssets.map {
        it.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    private val _accountsBalance = combine(
        session.accounts,
        session.settings(),
        merge(flowOf(Unit), session.accountsAndBalanceUpdated), // set initial value
        merge(flowOf(Unit), session.networkAssetManager.assetsUpdateFlow), // set initial value
        denomination
    ) { accounts, _, _, _, _ ->
        accounts.map {
            AccountBalance.create(account = it, session = session)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val accounts: StateFlow<List<AccountBalance>> =
        combine(hideAmounts, _accountsBalance, session.expired2FA) { hideAmounts, accountsBalance, _ ->
            if (hideAmounts) {
                accountsBalance.map { it.asMasked }
            } else {
                accountsBalance
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val lightningInfo: StateFlow<LightningInfoLook?> = (session.lightningSdkOrNull?.nodeInfoStateFlow?.map {
        if(session.isConnected && isLightningShortcut){
            LightningInfoLook.create(session = session, nodeState = it)
        } else null
    } ?: emptyFlow()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)


    private val _transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.walletTransactions.filter { session.isConnected },
        session.settings()
    ) { transactions, _ ->
        transactions.mapSuccess {
            it.map {
                TransactionLook.create(it, session)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DataState.Loading)

    // Re-calculate if needed (hideAmount or denomination & exchange rate change)
    override val transactions: StateFlow<DataState<List<TransactionLook>>> =
        combine(
            hideAmounts,
            _transactions
        ) { hideAmounts, transactionsLooks ->
            if (transactionsLooks is DataState.Success && hideAmounts) {
                DataState.Success(transactionsLooks.data.map { it.asMasked })
            } else {
                transactionsLooks
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DataState.Loading)

    private val primaryBalanceInFiat: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _systemMessage: MutableStateFlow<AlertType?> = MutableStateFlow(null)
    private val _twoFactorState: MutableStateFlow<AlertType?> = MutableStateFlow(null)

    override val alerts: StateFlow<List<AlertType>> = combine(
        _twoFactorState,
        _systemMessage,
        session.failedNetworks,
        session.lightningSdkOrNull?.healthCheckStatus ?: MutableStateFlow(null),
        banner
    ) { twoFactorState, systemMessage, failedNetworkLogins, lspHeath, banner ->
        listOfNotNull(
            twoFactorState,
            systemMessage,
            if (greenWallet.isBip39Ephemeral) AlertType.EphemeralBip39 else null,
            banner?.let { AlertType.Banner(it) },
            if (session.isTestnet) AlertType.TestnetWarning else null,
            AlertType.FailedNetworkLogin.takeIf { failedNetworkLogins.isNotEmpty() },
            lspHeath?.takeIf { it != HealthCheckStatus.OPERATIONAL }
                ?.let { AlertType.LspStatus(maintenance = it == HealthCheckStatus.MAINTENANCE) },
        )
    }.filter { session.isConnected }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val archivedAccounts: StateFlow<Int> = session.allAccounts.map {
        it.filter { it.hidden }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    override val isLightningShortcut
        get() = session.isLightningShortcut

    class LocalEvents {
        object ToggleBalance : Event
        object ToggleHideAmounts : Event
        object Refresh : Event
        object Send: Event
        object Receive: Event
        object DenominationExchangeRate: Event
        object ClickLightningLearnMore : Events.OpenBrowser(Urls.HELP_RECEIVE_CAPACITY)

        object OpenOptionsMenu: Event
        object MenuNewAccountClick: Event
    }

    class LocalSideEffects {
        object AccountArchivedDialog: SideEffect
    }

    init {
        // For animation reasons, set navData title immediately
        _navData.value = NavData(
            title = greenWallet.name
        )

        session.ifConnected {
            combine(greenWalletFlow.filterNotNull(), isWalletOnboarding) { greenWallet, isWalletOnboarding ->
                _navData.value = NavData(
                    title = greenWallet.name,
                    subtitle = if(session.isLightningShortcut) getString(Res.string.id_lightning_account) else null,
                    isVisible = !isWalletOnboarding,
                    actions = listOfNotNull(
                        NavAction(
                            title = "Menu",
                            icon = Res.drawable.dots_three_vertical_bold,
                            isMenuEntry = false,
                            onClick = {
                                postSideEffect(SideEffects.OpenDialog())
                            }
                        ),
                    )
                )
            }.launchIn(this)

            session.systemMessage.filter { session.isConnected }.onEach {
                _systemMessage.value = if (it.isEmpty()) {
                    null
                } else {
                    AlertType.SystemMessage(it.first().first, it.first().second)
                }
            }.launchIn(this)

            // Support only for Bitcoin
            session.bitcoinMultisig?.let { network ->
                session.twoFactorReset(network).filter { session.isConnected }.onEach {
                    _twoFactorState.value = if (it != null && it.isActive == true) {
                        if (it.isDisputed == true) {
                            AlertType.Dispute2FA(network, it)
                        } else {
                            AlertType.Reset2FA(network, it)
                        }
                    } else {
                        null
                    }
                }.launchIn(this)
            }

            combine(session.walletTotalBalance, session.walletAssets, primaryBalanceInFiat, hideAmounts, session.settings()) { _, _, _, _, _ ->
                session.isConnected
            }.filter { isConnected ->
                // Prevent from updating on non connected sessions
                isConnected
            }.onEach {
                updateBalance()
            }.launchIn(this)

            session.eventsSharedFlow.onEach {
                when(it){
                    WalletEvents.ARCHIVED_ACCOUNT -> {
                        postSideEffect(LocalSideEffects.AccountArchivedDialog)
                    }
                    WalletEvents.APP_REVIEW -> {
                        if (AppReviewHelper.shouldAskForReview(settingsManager, countly)) {
                            postSideEffect(SideEffects.AppReview)
                        }
                    }
                }
            }.launchIn(this)

            // Handle pending URI (BIP-21 or lightning)
            sessionManager.pendingUri.filterNotNull().debounce(50L).onEach {
                logger.d { "Handling pending intent in WalletOverviewViewModel" }
                // Check if pendingUri is consumed from SendViewModel
                if (sessionManager.pendingUri.value != null) {
                    handleUserInput(it, isQr = false)
                    sessionManager.pendingUri.value = null
                }
            }.launchIn(this)
        }

        bootstrap()
    }

    private suspend fun updateBalance() {
        // Loading
        if (session.walletTotalBalance.value == -1L) {
            _balancePrimary.value = null
            _balanceSecondary.value = null
        } else {
            val balance = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
                session = session,
                assetId = session.walletAssets.value.policyId
            )

            val fiat = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
                session = session,
                denomination = Denomination.fiat(session)
            )

            if (primaryBalanceInFiat.value) {
                _balancePrimary.value = fiat
                _balanceSecondary.value = balance
            } else {
                _balancePrimary.value = balance
                _balanceSecondary.value = fiat
            }
        }
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.OpenOptionsMenu -> {
                postSideEffect(SideEffects.OpenDialog())
            }
            is LocalEvents.MenuNewAccountClick -> {
                postEvent(Events.ChooseAccountType())
                countly.accountNew(session)
            }
            is LocalEvents.ToggleBalance -> {
                primaryBalanceInFiat.toggle()
            }

            is LocalEvents.Refresh -> {
                sessionOrNull?.refresh()
            }

            is Events.DismissSystemMessage -> {
                _systemMessage.value = null
            }

            is LocalEvents.Send -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        if (session.canSendTransaction) {
                            NavigateDestinations.Send()
                        } else {
                            NavigateDestinations.Sweep(
                                accountAsset = session.activeAccount.value?.accountAsset
                            )
                        }
                    )
                )
            }

            is LocalEvents.Receive -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Receive(accountAsset = session.activeAccount.value!!.accountAsset)))
            }

            is LocalEvents.DenominationExchangeRate -> {
                countly.preferredUnits(session)
                postSideEffect(SideEffects.OpenDenominationExchangeRate)
            }

            is LocalEvents.ToggleHideAmounts -> {
                settingsManager.saveApplicationSettings(
                    settingsManager.getApplicationSettings().let {
                        it.copy(hideAmounts = !it.hideAmounts)
                    }
                )

                if (settingsManager.appSettings.hideAmounts) {
                    countly.hideAmount(session)
                }
            }
        }
    }

    companion object: Loggable()
}

class WalletOverviewViewModelPreview(val isEmpty: Boolean = false) :
    WalletOverviewViewModelAbstract(greenWallet = previewWallet()) {

    override val isWalletOnboarding: StateFlow<Boolean> = MutableStateFlow(isEmpty)

    override val alerts: StateFlow<List<AlertType>> = MutableStateFlow(listOf())

    override val balancePrimary: StateFlow<String?> = MutableStateFlow("1.00000 BTC")

    override val balanceSecondary: StateFlow<String?> = MutableStateFlow("90.000 USD")

    override val hideAmounts: StateFlow<Boolean> = MutableStateFlow(false)

    override val assetsVisibility: StateFlow<Boolean?> = MutableStateFlow(true)

    override val activeAccount: StateFlow<Account?> = MutableStateFlow(null)

    override val assetIcons: StateFlow<List<String>> = MutableStateFlow(if(isEmpty) listOf() else
        listOf(
            EnrichedAsset.PreviewBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC
        ).map {
            it.assetId
        }
    )

    override val totalAssets: StateFlow<Int> = MutableStateFlow(7)

    override val accounts: StateFlow<List<AccountBalance>> = MutableStateFlow(
        listOf(
            AccountBalance.create(previewAccount()),
            AccountBalance.create(previewAccount())
        )
    )

    override val lightningInfo: StateFlow<LightningInfoLook?> = MutableStateFlow(null)

    override val transactions: StateFlow<DataState<List<TransactionLook>>> = MutableStateFlow(DataState.Success(if(isEmpty) listOf() else listOf(
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
    )))

    override val archivedAccounts: StateFlow<Int> = MutableStateFlow(1)

    companion object: Loggable(){
        fun create(isEmpty: Boolean = false) = WalletOverviewViewModelPreview(isEmpty = isEmpty)
    }
}
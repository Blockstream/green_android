package com.blockstream.common.models.overview

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_home
import blockstream_green.common.generated.resources.id_your_s_total_balance_is_the_sum_of
import breez_sdk.HealthCheckStatus
import com.blockstream.common.Urls
import com.blockstream.common.btcpricehistory.model.BitcoinChartData
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.DataState
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.common.extensions.previewTransactionLook
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.WalletEvents
import com.blockstream.common.lightning.onchainBalanceSatoshi
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.AppReviewHelper
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.green.domain.notifications.RegisterFCMToken
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class WalletOverviewViewModelAbstract(
    greenWallet: GreenWallet
) : WalletBalanceViewModel(greenWallet = greenWallet) {

    override fun screenName(): String = "WalletOverview"

    @NativeCoroutinesState
    abstract val transaction: StateFlow<DataState<TransactionLook?>>

    @NativeCoroutinesState
    abstract val alerts: StateFlow<List<AlertType>>

    @NativeCoroutinesState
    abstract val showWalletOnboarding: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val assets: StateFlow<DataState<List<AssetBalance>>>

    @NativeCoroutinesState
    abstract val activeAccount: StateFlow<Account?>

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<AccountAssetBalance>>

    @NativeCoroutinesState
    abstract val archivedAccounts: StateFlow<Int>

    @NativeCoroutinesState
    abstract val showHardwareTransferFunds: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val bitcoinChartData: StateFlow<BitcoinChartData?>

    abstract fun refetchBitcoinPriceHistory()

    fun openAssetAccounts(asset: EnrichedAsset) {
        viewModelScope.launch {
            session.accounts.value.filter {
                if (asset.isAmp) {
                    it.type == AccountType.AMP_ACCOUNT
                } else if (asset.assetId.isPolicyAsset(session)) {
                    it.network.policyAsset == asset.assetId
                } else {
                    it.isLiquid && (it.isAmp == it.isAmp)
                }
            }.also { accounts ->
                postEvent(
                    NavigateDestinations.Accounts(
                        greenWallet = greenWallet,
                        accounts = AccountAssetBalanceList(accounts.map {
                            AccountAssetBalance.create(
                                accountAsset = AccountAsset.fromAccountAsset(
                                    account = it, assetId = asset.assetId, session = session
                                ), session = session
                            )
                        }),
                        title = asset.name(session).getStringOrNull(),
                        message = getString(Res.string.id_your_s_total_balance_is_the_sum_of, asset.name(session).getString()),
                        withAsset = false,
                        withArrow = false,
                        withAssetIcon = false,
                        withAction = false,
                    )
                )
            }
        }
    }

    fun navigateToBuy() {
        postEvent(
            NavigateDestinations.Buy(
                greenWallet = greenWallet,
            )
        )
    }
}

class WalletOverviewViewModel(
    greenWallet: GreenWallet, showWalletOnboarding: Boolean = true
) : WalletOverviewViewModelAbstract(greenWallet = greenWallet) {
    private var refreshBitcoinPriceState = MutableStateFlow(0)

    private val observeBitcoinPriceHistory: ObserveBitcoinPriceHistory by inject()
    private val registerFCMToken: RegisterFCMToken by inject()
    private val fcmCommon: FcmCommon by inject()

    init {
        viewModelScope.launch {
            combine(refreshBitcoinPriceState, session.settings()) { _: Int, settings: Settings? ->
                settings?.pricing?.currency
            }.filterNotNull().collectLatest { currency ->
                observeBitcoinPriceHistory(
                    ObserveBitcoinPriceHistory.Params(
                        currency
                    )
                )
            }
        }

        // TODO move it to GDK session on the login procedure
        viewModelScope.launch {
            fcmCommon.token?.let { token ->
                registerFCMToken(
                    RegisterFCMToken.Params(
                        externalCustomerId = greenWallet.xPubHashId,
                        fcmToken = token,
                    )
                ).collect()
            }
        }
    }

    override fun segmentation(): HashMap<String, Any> =
        countly.sessionSegmentation(session = session)

    override val activeAccount: StateFlow<Account?> = session.activeAccount

    override val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), settingsManager.appSettings.hideAmounts
    )

    override val showWalletOnboarding: MutableStateFlow<Boolean> =
        MutableStateFlow(showWalletOnboarding)

    override val assets: StateFlow<DataState<List<AssetBalance>>> =
        combine(session.walletAssets, hideAmounts) { assets, hideAmounts ->
            assets.mapSuccess { assets ->
                assets.assets.map {
                    AssetBalance.create(
                        assetId = it.key, balance = it.value, session = session
                    )
                }
            }
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading
        )

    override val accounts = combine(
        session.accounts,
        session.settings(),
        merge(flowOf(Unit), session.accountsAndBalanceUpdated), // set initial value
        merge(flowOf(Unit), session.networkAssetManager.assetsUpdateFlow), // set initial value
        denomination
    ) { accounts, setting, _, _, _ ->
        // Set denomination directly from settings as sometimes the settings/network is not changed yet
        accounts.map {
            AccountAssetBalance.create(
                accountAsset = it.accountAsset,
                session = session,
                denomination = setting?.unit?.let { Denomination.byUnit(it) })
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    private val _transaction: StateFlow<DataState<TransactionLook?>> = combine(
        session.walletTransactions.filter { session.isConnected }, session.settings()
    ) { transactions, _ ->
        transactions.mapSuccess {
            it.firstOrNull()?.let {
                TransactionLook.create(
                    transaction = it, session = session, disableHideAmounts = true
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    // Re-calculate if needed (hideAmount or denomination & exchange rate change)
    override val transaction: StateFlow<DataState<TransactionLook?>> = combine(
        hideAmounts, _transaction
    ) { hideAmounts, transactionsLook ->
        if (transactionsLook is DataState.Success && hideAmounts) {
            DataState.Success(transactionsLook.data?.asMasked)
        } else {
            transactionsLook
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    private val _systemMessage: MutableStateFlow<AlertType?> = MutableStateFlow(null)
    private val _twoFactorState: MutableStateFlow<AlertType?> = MutableStateFlow(null)

    private val hideWalletBackupAlert = MutableStateFlow(false)

    override val alerts: StateFlow<List<AlertType>> = com.blockstream.common.extensions.combine(
        greenWalletFlow.filterNotNull()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), greenWallet),
        _twoFactorState,
        _systemMessage,
        session.failedNetworks,
        session.lightningSdkOrNull?.healthCheckStatus ?: MutableStateFlow(null),
        banner,
        hideWalletBackupAlert,
        session.walletTotalBalance
    ) { greenWallet, twoFactorState, systemMessage, failedNetworkLogins, lspHeath, banner, hideWalletBackupAlert, walletTotalBalance ->
        listOfNotNull(
            if (!greenWallet.isRecoveryConfirmed && !hideWalletBackupAlert && walletTotalBalance > 0) AlertType.RecoveryIsUnconfirmed(
                withCloseButton = true
            ) else null,
            twoFactorState,
            systemMessage,
            if (greenWallet.isBip39Ephemeral) AlertType.EphemeralBip39 else null,
            banner?.let { AlertType.Banner(it) },
            if (session.isTestnet) AlertType.TestnetWarning else null,
            AlertType.FailedNetworkLogin.takeIf { failedNetworkLogins.isNotEmpty() },
            lspHeath?.takeIf { it != HealthCheckStatus.OPERATIONAL }
                ?.let { AlertType.LspStatus(maintenance = it == HealthCheckStatus.MAINTENANCE) },
        )
    }.filter { session.isConnected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), listOf())

    override val archivedAccounts: StateFlow<Int> = session.allAccounts.map {
        it.filter { it.hidden && it.hasHistory(session) }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    override val showHardwareTransferFunds = transaction.map {
        sessionOrNull?.isHardwareWallet == true && (it is DataState.Success && it.data == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    override val bitcoinChartData = observeBitcoinPriceHistory.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    override fun refetchBitcoinPriceHistory() {
        refreshBitcoinPriceState.value++
    }


    class LocalEvents {
        object Refresh : Event
        object Send : Event
        object Receive : Event
        object DenominationExchangeRate : Event
        object ClickLightningSweep : Event
        object ClickLightningLearnMore : Events.OpenBrowser(Urls.HELP_RECEIVE_CAPACITY)

        object OpenOptionsMenu : Event
        object MenuNewAccountClick : Event
    }

    class LocalSideEffects {
        object AccountArchivedDialog : SideEffect
    }

    init {
        session.ifConnected {
            combine(
                greenWalletFlow.filterNotNull(), this.showWalletOnboarding, session.isWatchOnly
            ) { greenWallet, _, _ ->
                updateNavData(greenWallet)
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

            session.eventsSharedFlow.onEach {
                when (it) {
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

    private suspend fun updateNavData(greenWallet: GreenWallet) {
        _navData.value = NavData(
            title = getString(Res.string.id_home),
            walletName = greenWallet.name,
            isVisible = !showWalletOnboarding.value,
            showBadge = !greenWallet.isRecoveryConfirmed,
            showBottomNavigation = !showWalletOnboarding.value
        )
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is Events.DismissWalletBackupAlert -> {
                viewModelScope.launch {
                    hideWalletBackupAlert.value = true
                }
            }

            is LocalEvents.OpenOptionsMenu -> {
                postSideEffect(SideEffects.OpenDialog())
            }

            is LocalEvents.MenuNewAccountClick -> {
                postEvent(Events.ChooseAccountType())
                countly.accountNew(session)
            }

            is LocalEvents.Refresh -> {
                sessionOrNull?.refresh()
                refetchBitcoinPriceHistory()
            }

            is Events.DismissSystemMessage -> {
                _systemMessage.value = null
            }

            is LocalEvents.Send -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        if (session.canSendTransaction) {
                            NavigateDestinations.Send(greenWallet = greenWallet)
                        } else {
                            NavigateDestinations.Sweep(
                                greenWallet = greenWallet,
                                accountAsset = session.activeAccount.value?.accountAsset
                            )
                        }
                    )
                )
            }

            is LocalEvents.Receive -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Receive(
                            accountAsset = session.activeAccount.value!!.accountAsset,
                            greenWallet = greenWallet
                        )
                    )
                )
            }

            is LocalEvents.DenominationExchangeRate -> {
                countly.preferredUnits(session)
                postSideEffect(SideEffects.OpenDenominationExchangeRate)
            }

            is LocalEvents.ClickLightningSweep -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoverFunds(
                            greenWallet = greenWallet,
                            amount = session.lightningSdk.nodeInfoStateFlow.value.onchainBalanceSatoshi()
                        )
                    )
                )
            }
        }
    }

    companion object : Loggable()
}

class WalletOverviewViewModelPreview(val isEmpty: Boolean = false, val isHardware: Boolean = true) :
    WalletOverviewViewModelAbstract(greenWallet = previewWallet(isHardware = isHardware)) {

    override val transaction: StateFlow<DataState<TransactionLook?>> = MutableStateFlow(
        DataState.Success(
            if (isEmpty) null else previewTransactionLook()
        )
    )

    override val showWalletOnboarding: MutableStateFlow<Boolean> = MutableStateFlow(isEmpty)

    override val alerts: StateFlow<List<AlertType>> = MutableStateFlow(listOf())

    override val balancePrimary: StateFlow<String?> = MutableStateFlow("1.00000 BTC")

    override val balanceSecondary: StateFlow<String?> = MutableStateFlow("90.000 USD")

    override val hideAmounts: StateFlow<Boolean> = MutableStateFlow(false)

    override val activeAccount: StateFlow<Account?> = MutableStateFlow(null)

    override val assets: StateFlow<DataState<List<AssetBalance>>> = MutableStateFlow(
        DataState.successOrEmpty(
            listOf(
                previewAssetBalance(), previewAssetBalance(isLiquid = true)
            )
        )
    )

    override val accounts: StateFlow<List<AccountAssetBalance>> = MutableStateFlow(
        listOf(
            AccountAssetBalance.create(previewAccountAsset()),
            AccountAssetBalance.create(previewAccountAsset())
        )
    )

    override val archivedAccounts: StateFlow<Int> = MutableStateFlow(1)

    override val showHardwareTransferFunds: StateFlow<Boolean> = MutableStateFlow(true)

    override val bitcoinChartData: StateFlow<BitcoinChartData?> = MutableStateFlow(
        null
    )

    override fun refetchBitcoinPriceHistory() {
        // No-op
    }


    companion object : Loggable() {
        fun create(isEmpty: Boolean = false) = WalletOverviewViewModelPreview(isEmpty = isEmpty)
    }
}
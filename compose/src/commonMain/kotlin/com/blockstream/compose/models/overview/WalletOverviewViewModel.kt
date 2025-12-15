package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_home
import breez_sdk.HealthCheckStatus
import com.blockstream.data.btcpricehistory.model.BitcoinChartData
import com.blockstream.data.data.AlertType
import com.blockstream.data.data.DataState
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.filterForAsset
import com.blockstream.data.extensions.hasHistory
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.data.gdk.data.Settings
import com.blockstream.data.gdk.data.WalletEvents
import com.blockstream.data.utils.AppReviewHelper
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewAssetBalance
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.looks.transaction.TransactionLook
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.domain.notifications.RegisterFCMToken
import com.blockstream.utils.Loggable
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class WalletOverviewViewModelAbstract(
    greenWallet: GreenWallet
) : WalletBalanceViewModel(greenWallet = greenWallet) {

    override fun screenName(): String = "HomeTab"
    abstract val alerts: StateFlow<List<AlertType>>
    abstract val showWalletOnboarding: MutableStateFlow<Boolean>
    abstract val assets: StateFlow<DataState<List<AssetBalance>>>
    abstract val activeAccount: StateFlow<Account?>
    abstract val accounts: StateFlow<List<AccountAssetBalance>>
    abstract val archivedAccounts: StateFlow<Int>
    abstract val bitcoinChartData: StateFlow<DataState<BitcoinChartData>?>
    abstract fun refetchBitcoinPriceHistory()

    fun openAssetAccounts(asset: EnrichedAsset) {
        val accountsWithAsset = session.accounts.value.filterForAsset(asset.assetId, session)

        if (accountsWithAsset.size == 1) {
            accountsWithAsset.firstOrNull()?.let { account ->
                postEvent(
                    NavigateDestinations.AssetAccountDetails(
                        greenWallet = greenWallet,
                        accountAsset = AccountAsset(account, asset)
                    )
                )
            }
        } else {
            postEvent(
                NavigateDestinations.AssetAccountList(
                    greenWallet = greenWallet,
                    assetId = asset.assetId
                )
            )
        }
    }

    fun navigateToBuy() {
        postEvent(
            NavigateDestinations.Buy(
                greenWallet = greenWallet,
            )
        )
        countly.buyInitiate()
    }

    fun dismissWalletOnboarding() {
        countly.swwCreated()
        showWalletOnboarding.value = false
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

    private val _systemMessage: MutableStateFlow<AlertType?> = MutableStateFlow(null)
    private val _twoFactorState: MutableStateFlow<AlertType?> = MutableStateFlow(null)

    private val hideWalletBackupAlert = MutableStateFlow(false)

    override val alerts: StateFlow<List<AlertType>> = com.blockstream.data.extensions.combine(
        greenWalletFlow.filterNotNull()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), greenWallet),
        _twoFactorState,
        _systemMessage,
        session.expired2FA,
        session.failedNetworks,
        session.lightningSdkOrNull?.healthCheckStatus ?: MutableStateFlow(null),
        banner,
        hideWalletBackupAlert,
        session.walletTotalBalance
    ) { greenWallet, twoFactorState, systemMessage, expired2FA, failedNetworkLogins, lspHeath, banner, hideWalletBackupAlert, walletTotalBalance ->
        listOfNotNull(
            if (!greenWallet.isRecoveryConfirmed && !hideWalletBackupAlert && walletTotalBalance > 0) AlertType.RecoveryIsUnconfirmed(
                withCloseButton = true
            ) else null,
            twoFactorState,
            systemMessage,
            if (expired2FA.isNotEmpty()) AlertType.ReEnable2FA else null,
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

    override val bitcoinChartData = observeBitcoinPriceHistory.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    override fun refetchBitcoinPriceHistory() {
        refreshBitcoinPriceState.value++
    }

    class LocalEvents {
        object Refresh : Event
        object DenominationExchangeRate : Event
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
                    _twoFactorState.value = if (it?.isActive == true) {
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
            
            is LocalEvents.DenominationExchangeRate -> {
                countly.preferredUnits(session)
                postSideEffect(SideEffects.OpenDenominationExchangeRate)
            }
        }
    }

    companion object : Loggable()
}

class WalletOverviewViewModelPreview(val isEmpty: Boolean = false, val isHardware: Boolean = true) :
    WalletOverviewViewModelAbstract(greenWallet = previewWallet(isHardware = isHardware)) {

    override val showWalletOnboarding: MutableStateFlow<Boolean> = MutableStateFlow(isEmpty)

    override val alerts: StateFlow<List<AlertType>> = MutableStateFlow(listOf())

    override val balancePrimary: StateFlow<String?> = MutableStateFlow("1.00000 BTC")

    // override val balanceSecondary: StateFlow<String?> = MutableStateFlow("90.000 USD")

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

    override val bitcoinChartData: StateFlow<DataState<BitcoinChartData>?> = MutableStateFlow(
        null
    )

    override fun refetchBitcoinPriceHistory() {
        // No-op
    }

    companion object : Loggable() {
        fun create(isEmpty: Boolean = false) = WalletOverviewViewModelPreview(isEmpty = isEmpty)
    }
}

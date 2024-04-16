package com.blockstream.common.models.overview

import breez_sdk.HealthCheckStatus
import com.blockstream.common.Urls
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.DataState
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.lightningMnemonic
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewAccountBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.lightning.fromSwapInfo
import com.blockstream.common.lightning.onchainBalanceSatoshi
import com.blockstream.common.looks.account.LightningInfoLook
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

abstract class AccountOverviewViewModelAbstract(
    greenWallet: GreenWallet, accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {

    override fun screenName(): String = "AccountOverview"

    @NativeCoroutinesState
    abstract val hasLightningShortcut: StateFlow<Boolean?>

    @NativeCoroutinesState
    abstract val alerts: StateFlow<List<AlertType>>

    @NativeCoroutinesState
    abstract val assets: StateFlow<DataState<List<AssetBalance>>>

    @NativeCoroutinesState
    abstract val accountBalance: StateFlow<AccountBalance>

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<Account>>

    @NativeCoroutinesState
    abstract val showAmpInfo: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val lightningInfo: StateFlow<LightningInfoLook?>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>

    @NativeCoroutinesState
    abstract val hasMoreTransactions: StateFlow<Boolean>
}

class AccountOverviewViewModel(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    AccountOverviewViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override fun segmentation(): HashMap<String, Any> =
        countly.accountSegmentation(session = session, account = account)

    override val hasLightningShortcut = if (greenWallet.isEphemeral) {
        emptyFlow<Boolean?>()
    } else {
        database.getLoginCredentialsFlow(greenWallet.id).map {
            it.lightningMnemonic != null
        }
    }.filter { session.isConnected }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val assets: StateFlow<DataState<List<AssetBalance>>> = session.accountAssets(account).map { assets ->
        session.takeIf { account.isLiquid && assets.size > 1}?.ifConnected {
            DataState.Success(assets.assets.map {
                AssetBalance.create(
                    assetId = it.key,
                    balance = it.value,
                    session = session
                )
            })
        } ?: DataState.Empty
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), if(account.isLiquid) DataState.Loading else DataState.Empty)

    override val accountBalance: StateFlow<AccountBalance> = this.accountAsset
        .filterNotNull()
        .filter { session.isConnected }
        .map {
            AccountBalance.create(account = it.account, session = session)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            accountAsset.account.accountBalance
        )

    override val showAmpInfo: StateFlow<Boolean> = assets.map {
        account.isAmp && it.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    override val accounts: StateFlow<List<Account>> =
        session.accounts.filter { session.isConnected }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val lightningInfo: StateFlow<LightningInfoLook?> =
        session.lightningNodeInfoStateFlow.map {
            if (session.isConnected && account.isLightning) {
                LightningInfoLook.create(session = session, nodeState = it)
            } else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.accountTransactions(account),
        (session.takeIf { account.isLightning }?.ifConnected {
            session.lightningSdkOrNull?.swapInfoStateFlow
        } ?: flowOf(listOf()))) { accountTransactions, swaps ->

        if (accountTransactions.isSuccess()) {
            (swaps.map {
                Transaction.fromSwapInfo(account, it.first, it.second)
            } + (accountTransactions.data() ?: listOf())).let { DataState.Success(it) }
        } else {
            accountTransactions
        }
    }.filter { session.isConnected }
        .map { transactionsLooks ->
            val hideAmounts = settingsManager.appSettings.hideAmounts
            transactionsLooks.mapSuccess {
                it.map {
                    TransactionLook.create(it, session).let { look ->
                        if (hideAmounts) {
                            look.asMasked
                        } else look
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DataState.Loading)

    override val hasMoreTransactions = session.accountTransactionsPager(account)

    private val _twoFactorState: MutableStateFlow<AlertType?> = MutableStateFlow(null)

    override val alerts: StateFlow<List<AlertType>> = combine(
        _twoFactorState,
        session.lightningSdkOrNull?.healthCheckStatus.takeIf { account.isLightning }
            ?: MutableStateFlow(null),
        banner
    ) { twoFactorState, lspHeath, banner ->
        listOfNotNull(
            twoFactorState,
            lspHeath?.takeIf { it != HealthCheckStatus.OPERATIONAL }
                ?.let { AlertType.LspStatus(maintenance = it == HealthCheckStatus.MAINTENANCE) },
        )
    }.filter { session.isConnected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    class LocalEvents {
        object Send : Event
        object Receive : Event
        object Refresh : Event
        object RemoveLightningShortcut : Event
        object EnableLightningShortcut : Event
        object LoadMoreTransactions : Event
        object RescanSwaps : Event
        object CopyAccountId : Event
        object ClickLightningSweep : Event
        object ClickLightningLearnMore : Events.OpenBrowser(Urls.HELP_RECEIVE_CAPACITY)
    }

    init {
        session.ifConnected {
            combine(this.accountAsset, hasLightningShortcut, accounts) { accountAsset, hasLightningShortcut, accounts ->
                _navData.value = NavData(
                    title = greenWallet.name,
                    subtitle = accountAsset?.account?.name,
                    actions = listOfNotNull(
                        NavAction(
                            title = "id_help",
                            icon = "question",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(Events.OpenBrowser(Urls.HELP_AMP_ASSETS))
                            }
                        ).takeIf { account.isAmp },
                        NavAction(
                            title = "id_rename_account",
                            icon = "text_aa",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(NavigateDestinations.RenameAccount(account))
                            }
                        ).takeIf { !session.isWatchOnly && !account.isLightning },
                        NavAction(
                            title = "id_archive_account",
                            icon = "box_arrow_down",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(Events.ArchiveAccount(account))
                            }
                        ).takeIf { !session.isWatchOnly && !account.isLightning && accounts.size > 1 },
                        NavAction(
                            title = "id_node_info",
                            icon = "info",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(NavigateDestinations.LightningNode)
                            }
                        ).takeIf { account.isLightning },
                        NavAction(
                            title = "id_add_lightning_shortcut",
                            icon = "lightning",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(LocalEvents.EnableLightningShortcut)
                            }
                        ).takeIf { account.isLightning && hasLightningShortcut == false },
                        NavAction(
                            title = "id_remove_lightning_shortcut",
                            icon = "ic_lightning_slash",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(LocalEvents.RemoveLightningShortcut)
                            }
                        ).takeIf { account.isLightning &&  hasLightningShortcut == true },
                        NavAction(
                            title = "id_remove",
                            icon = "trash",
                            isMenuEntry = true,
                            onClick = {
                                postEvent(Events.RemoveAccount(account))
                            }
                        ).takeIf { account.isLightning },
                    )
                )
            }.launchIn(this)

            session.twoFactorReset(account.network).onEach {
                _twoFactorState.value = if (it != null && it.isActive == true) {
                    if (it.isDisputed == true) {
                        AlertType.Dispute2FA(account.network, it)
                    } else {
                        AlertType.Reset2FA(account.network, it)
                    }
                } else {
                    null
                }
            }.filter { session.isConnected }.launchIn(this)

            session.getTransactions(account = account, isReset = true, isLoadMore = false)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.Receive -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Receive(
                            accountAsset = accountAsset.value!!
                        )
                    )
                )
            }

            is LocalEvents.Send -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        if (greenWallet.isWatchOnly) {
                            NavigateDestinations.Sweep(
                                accountAsset = accountAsset.value,
                            )
                        } else {
                            NavigateDestinations.Send(
                                accountAsset = accountAsset.value!!,
                            )
                        }
                    )
                )
            }

            is LocalEvents.Refresh -> {
                session.refresh(account = account)
            }

            is LocalEvents.RemoveLightningShortcut -> {
                removeLightningShortcut()
            }

            is LocalEvents.EnableLightningShortcut -> {
                enableLightningShortcut()
            }

            is LocalEvents.LoadMoreTransactions -> {
                loadMoreTransactions()
            }

            is LocalEvents.RescanSwaps -> {
                rescanSwaps()
            }

            is LocalEvents.CopyAccountId -> {
                postSideEffect(SideEffects.CopyToClipboard(value = account.receivingId, message = "id_copied_to_clipboard", label = "Account ID"))
            }

            is LocalEvents.ClickLightningSweep -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoverFunds(
                            satoshi = session.lightningSdk.nodeInfoStateFlow.value.onchainBalanceSatoshi()
                        )
                    )
                )
            }
        }
    }

    private fun loadMoreTransactions() {
        logger.i { "Load more transactions" }
        session.getTransactions(account = account, isReset = false, isLoadMore = true)
    }

    private fun enableLightningShortcut() {
        if (account.isLightning) {
            doAsync({
                _enableLightningShortcut()
            }, onSuccess = {
                postSideEffect(SideEffects.LightningShortcut)
            })
        }
    }

    private fun removeLightningShortcut() {
        if (account.isLightning) {
            doAsync({
                database.deleteLoginCredentials(greenWallet.id, CredentialType.LIGHTNING_MNEMONIC)
            }, onSuccess = {

            })
        }
    }

    private fun rescanSwaps() {
        postSideEffect(SideEffects.Snackbar("id_rescan_swaps_initiated"))

        doAsync({
            session.lightningSdkOrNull?.rescanSwaps()
        }, onSuccess = {

        })
    }

    companion object : Loggable()
}

class AccountOverviewViewModelPreview() : AccountOverviewViewModelAbstract(
    greenWallet = previewWallet(),
    accountAsset = previewAccountAsset()
) {
    override val hasLightningShortcut = MutableStateFlow(false)
    override val alerts: StateFlow<List<AlertType>> = MutableStateFlow(listOf())

    override val assets: StateFlow<DataState<List<AssetBalance>>> = MutableStateFlow(DataState.Success(listOf(
        AssetBalance(EnrichedAsset.PreviewBTC, "1 BTC", "1000USD"),
        AssetBalance(EnrichedAsset.PreviewLBTC, "1 L-BTC", "1000USD")
    )))

    override val showAmpInfo: StateFlow<Boolean> = MutableStateFlow(true)

    override val accountBalance: StateFlow<AccountBalance> =
        MutableStateFlow(previewAccountBalance())
    override val accounts: StateFlow<List<Account>> = MutableStateFlow(listOf(previewAccount()))
    override val lightningInfo: StateFlow<LightningInfoLook?> = MutableStateFlow(null)
    override val transactions: StateFlow<DataState<List<TransactionLook>>> =
        MutableStateFlow(DataState.Loading)
    override val hasMoreTransactions: StateFlow<Boolean> = MutableStateFlow(false)

    companion object : Loggable() {
        fun create() = AccountOverviewViewModelPreview()
    }
}
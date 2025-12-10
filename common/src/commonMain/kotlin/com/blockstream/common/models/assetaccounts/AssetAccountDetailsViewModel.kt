package com.blockstream.common.models.assetaccounts

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.binoculars
import blockstream_green.common.generated.resources.box_arrow_down
import blockstream_green.common.generated.resources.id_archive_account
import blockstream_green.common.generated.resources.id_node_info
import blockstream_green.common.generated.resources.id_rename_account
import blockstream_green.common.generated.resources.id_watchonly
import blockstream_green.common.generated.resources.info
import blockstream_green.common.generated.resources.text_aa
import com.blockstream.common.data.DataState
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.hasUnconfirmedTransactions
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.common.lightning.onchainBalanceSatoshi
import com.blockstream.common.looks.account.LightningInfoLook
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.toAmountLook
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class AssetAccountDetailsViewModelAbstract(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "AssetAccountDetails"

    abstract val asset: EnrichedAsset

    abstract val accountBalance: StateFlow<AccountBalance>

    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>

    abstract val totalBalance: StateFlow<String>

    abstract val totalBalanceFiat: StateFlow<String?>

    abstract val showBuyButton: Boolean

    abstract val isSendEnabled: StateFlow<Boolean>

    abstract val hasMoreTransactions: StateFlow<Boolean>

    abstract val accounts: StateFlow<List<Account>>

    abstract val lightningInfo: StateFlow<LightningInfoLook?>

    fun clickLightningSweep() {
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

class AssetAccountDetailsViewModel(
    greenWallet: GreenWallet, accountAsset: AccountAsset
) : AssetAccountDetailsViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAsset) {

    class LocalEvents {
        object ClickBuy : Event
        object ClickSend : Event
        object ClickReceive : Event
        object LoadMoreTransactions : Event
    }

    override val asset: EnrichedAsset = accountAsset.asset

    override val accountBalance: StateFlow<AccountBalance> = merge(flowOf(Unit), session.accountsAndBalanceUpdated).map {
        AccountBalance.create(account = account, session = session)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AccountBalance.create(account = account))

    override val showBuyButton: Boolean = accountAsset.asset.isBitcoin

    override val accounts: StateFlow<List<Account>> =
        session.accounts.filter { session.isConnected }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    private val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), settingsManager.appSettings.hideAmounts
    )

    override val isSendEnabled: StateFlow<Boolean> = combine(accountBalance, isMultisigWatchOnly) { accountBalance, isMultisigWatchOnly ->
        if (isMultisigWatchOnly) {
            false
        } else if (accountAsset.account.isLightning) {
            (session.lightningSdk.balanceOnChannel() ?: 0) > 0
        } else {
            accountBalance.balance(session) > 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    override val lightningInfo: StateFlow<LightningInfoLook?> = ((if (accountAsset.account.isLightning) {
        session.lightningSdkOrNull?.nodeInfoStateFlow?.map {
            if (session.isConnected) {
                LightningInfoLook.create(session = session, nodeState = it)
            } else null
        }
    } else null) ?: emptyFlow()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _totalBalance = MutableStateFlow("")
    override val totalBalance: StateFlow<String> = _totalBalance

    private val _totalBalanceFiat = MutableStateFlow<String?>(null)
    override val totalBalanceFiat: StateFlow<String?> = _totalBalanceFiat

    init {
        session.ifConnected {
            combine(accounts, isWatchOnly) { accounts, watchOnly ->
                viewModelScope.launch {
                    val assetName = accountAsset.asset.name(session).toString()
                    val accountName = accountAsset.account.name
                    _navData.value = NavData(
                        title = assetName,
                        subtitle = accountName,
                        actions = getMenuActions(
                            account = account,
                            accountAsset = accountAsset,
                            watchOnly = watchOnly
                        )
                    )
                }
            }.launchIn(viewModelScope.coroutineScope)
        }

        session.ifConnected {
            accountBalance.onEach {
                updateTotalBalance()
            }.launchIn(viewModelScope.coroutineScope)

            session.getTransactions(account = account, isReset = true, isLoadMore = false)
        }

        bootstrap()
    }

    private val _transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        session.accountTransactions(account), session.settings()
    ) { transactions, _ ->
        DataState.Success(
            (transactions.data() ?: emptyList()).map { transaction ->
                TransactionLook.create(
                    transaction = transaction, session = session
                )
            })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    override val transactions: StateFlow<DataState<List<TransactionLook>>> = combine(
        hideAmounts, _transactions
    ) { hideAmounts, transactionsLooks ->
        if (transactionsLooks is DataState.Success && hideAmounts) {
            DataState.Success(transactionsLooks.data.map { it.asMasked })
        } else {
            transactionsLooks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataState.Loading)

    override val hasMoreTransactions: StateFlow<Boolean> = session.accountTransactionsPager(account)

    private fun updateTotalBalance() {
        viewModelScope.launch {
            accountAsset.value?.let { accountAsset ->
                _totalBalance.value = accountAsset.balance(session).toAmountLook(
                    session = session, assetId = accountAsset.assetId, withUnit = true, withGrouping = true, withMinimumDigits = false
                ) ?: ""

                _totalBalanceFiat.value = accountAsset.balance(session).toAmountLook(
                    session = session, assetId = accountAsset.assetId, withUnit = true, denomination = Denomination.fiat(session)
                )?.let { fiatBalance ->
                    _totalBalance.value.takeIf { it.isNotBlank() && it != fiatBalance }?.let {
                        fiatBalance
                    }
                }
            }
        }
    }

    fun buy() {
        countly.buyInitiate()
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.Buy(
                    greenWallet = greenWallet,
                    accountAsset = accountAsset.value
                )
            )
        )
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClickBuy -> {
                buy()
            }

            is LocalEvents.ClickSend -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendAddress(
                            greenWallet = greenWallet, accountAsset = accountAsset.value
                        )
                    )
                )
            }

            is LocalEvents.ClickReceive -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Receive(
                            greenWallet = greenWallet, accountAsset = accountAsset.value!!
                        )
                    )
                )
            }

            is LocalEvents.LoadMoreTransactions -> {
                loadMoreTransactions()
            }
        }
    }

    private fun loadMoreTransactions() {
        session.getTransactions(account = account, isReset = false, isLoadMore = true)
    }

    private suspend fun getMenuActions(
        account: Account,
        accountAsset: AccountAsset?,
        watchOnly: Boolean
    ): List<NavAction> {

        if (account.isLightning) {
            return listOfNotNull(
                NavAction(
                    title = getString(Res.string.id_node_info),
                    icon = Res.drawable.info,
                    isMenuEntry = true,
                    onClick = {
                        postEvent(NavigateDestinations.LightningNode(greenWallet))
                    }
                )
            )
        }

        return listOfNotNull(
            NavAction(
                title = getString(Res.string.id_rename_account),
                icon = Res.drawable.text_aa,
                isMenuEntry = true,
                onClick = {
                    postEvent(NavigateDestinations.RenameAccount(greenWallet = greenWallet, account = account))
                }
            ).takeIf { !watchOnly },

            NavAction(
                title = getString(Res.string.id_watchonly),
                icon = Res.drawable.binoculars,
                isMenuEntry = true,
                onClick = {
                    if (account.isSinglesig) {
                        accountAsset?.also {
                            postEvent(NavigateDestinations.AccountDescriptor(greenWallet = greenWallet, accountAsset = it))
                        }
                    } else {
                        // For multisig accounts, show the watch-only credentials bottom sheet
                        postEvent(NavigateDestinations.WatchOnlyCredentialsSettings(greenWallet = greenWallet, network = account.network))
                    }
                }
            ),
            NavAction(
                title = getString(Res.string.id_archive_account),
                icon = Res.drawable.box_arrow_down,
                isMenuEntry = true,
                enabled = !account.isFunded(session) && !account.hasUnconfirmedTransactions(session),
                onClick = {
                    postEvent(Events.ArchiveAccount(account))
                }
            ).takeIf { !watchOnly }
        )
    }

    companion object : Loggable()
}

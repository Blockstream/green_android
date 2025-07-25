package com.blockstream.common.models.assetaccounts

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.binoculars
import blockstream_green.common.generated.resources.box_arrow_down
import blockstream_green.common.generated.resources.id_archive_account
import blockstream_green.common.generated.resources.id_rename_account
import blockstream_green.common.generated.resources.id_watchonly
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
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.toAmountLook
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class AssetAccountDetailsViewModelAbstract(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "AssetAccountDetails"

    @NativeCoroutinesState
    abstract val asset: StateFlow<EnrichedAsset?>

    @NativeCoroutinesState
    abstract val accountBalance: StateFlow<AccountBalance>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<DataState<List<TransactionLook>>>

    @NativeCoroutinesState
    abstract val totalBalance: StateFlow<String>

    @NativeCoroutinesState
    abstract val totalBalanceFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val showBuyButton: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val hasMoreTransactions: StateFlow<Boolean>
    
    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<Account>>
}

class AssetAccountDetailsViewModel(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset
) : AssetAccountDetailsViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    class LocalEvents {
        object ClickBuy : Event
        object ClickSend : Event
        object ClickReceive : Event
        object LoadMoreTransactions : Event
    }

    override val asset: StateFlow<EnrichedAsset?> =
        accountAsset.map { it?.asset }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), accountAsset.value?.asset)

    override val accountBalance: StateFlow<AccountBalance> = session.accountsAndBalanceUpdated.map {
        AccountBalance.create(account = account, session = session)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AccountBalance.create(account = account))

    override val showBuyButton: StateFlow<Boolean> = accountAsset.map {
        it?.asset?.isBitcoin == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), accountAsset.value?.asset?.isBitcoin == true)
    
    override val accounts: StateFlow<List<Account>> =
        session.accounts.filter { session.isConnected }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    private val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), settingsManager.appSettings.hideAmounts
    )

    private val _totalBalance = MutableStateFlow("")
    override val totalBalance: StateFlow<String> = _totalBalance

    private val _totalBalanceFiat = MutableStateFlow<String?>(null)
    override val totalBalanceFiat: StateFlow<String?> = _totalBalanceFiat

    init {
        session.ifConnected {
            combine(accountAsset, accounts, isWatchOnly) { accountAsset, accounts, watchOnly ->
                viewModelScope.launch {
                    val assetName = accountAsset?.asset?.name(session)?.toString() ?: accountAsset?.assetId ?: ""
                    val accountName = accountAsset?.account?.name ?: account.name
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
                    greenWallet = greenWallet
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
                        NavigateDestinations.Send(
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
        if (account.isLightning || watchOnly) {
            return emptyList()
        }

        return listOfNotNull(
            NavAction(
                title = getString(Res.string.id_rename_account),
                icon = Res.drawable.text_aa,
                isMenuEntry = true,
                onClick = {
                    postEvent(NavigateDestinations.RenameAccount(greenWallet = greenWallet, account = account))
                }
            ),
            
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
            )
        )
    }

    companion object : Loggable()
}

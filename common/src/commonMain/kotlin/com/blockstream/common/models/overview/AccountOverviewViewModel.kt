package com.blockstream.common.models.overview

import breez_sdk.HealthCheckStatus
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.lightningMnemonic
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.lightning.fromSwapInfo
import com.blockstream.common.looks.account.AccountLook
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
    abstract val assets: StateFlow<Map<EnrichedAsset, Long>>

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<AccountLook>>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<List<Transaction>>

    @NativeCoroutinesState
    abstract val hasMoreTransactions: StateFlow<Boolean>
}

class AccountOverviewViewModel(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    AccountOverviewViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override fun segmentation(): HashMap<String, Any> =
        countly.accountSegmentation(session = session, account = account)


    override val hasLightningShortcut = if(greenWallet.isEphemeral) {
        emptyFlow<Boolean?>()
    } else {
        database.getLoginCredentialsFlow(greenWallet.id).map {
            it.lightningMnemonic != null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val assets: StateFlow<Map<EnrichedAsset, Long>> = session.accountAssets(account).map {
        session.takeIf { account.isLiquid }?.ifConnected {
            it.toEnrichedAssets(session)
        } ?: mapOf()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), mapOf())

    override val accounts: StateFlow<List<AccountLook>> = session.accounts.map { accounts ->
        accounts.map {
            AccountLook.create(it)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val transactions: StateFlow<List<Transaction>> = combine(session.accountTransactions(account), (session.takeIf { account.isLightning }?.ifConnected {
        session.lightningSdkOrNull?.swapInfoStateFlow
    } ?: flowOf(listOf()))) { accountTransactions, swaps ->
        swaps.map {
            Transaction.fromSwapInfo(account, it.first, it.second)
        } + accountTransactions
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf(Transaction.LoadingTransaction))

    override val hasMoreTransactions = session.accountTransactionsPager(account).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _twoFactorState: MutableStateFlow<AlertType?> = MutableStateFlow(null)

    override val alerts: StateFlow<List<AlertType>> = combine(
        _twoFactorState,
        session.lightningSdkOrNull?.healthCheckStatus.takeIf { account.isLightning } ?: MutableStateFlow(null),
        banner
    ) { twoFactorState, lspHeath, banner ->
        listOfNotNull(
            twoFactorState,
            lspHeath?.takeIf { it != HealthCheckStatus.OPERATIONAL }
                ?.let { AlertType.LspStatus(maintenance = it == HealthCheckStatus.MAINTENANCE) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    class LocalEvents {
        object Send : Event
        object Receive : Event
        object Refresh : Event
        object RemoveLightningShortcut : Event
        object EnableLightningShortcut : Event
        object LoadMoreTransactions : Event
    }

    init {
        session.ifConnected {
            _navData.value = NavData(
                title = greenWallet.name,
                subtitle = if(session.isLightningShortcut) "id_lightning_account" else null,
//                actions = listOfNotNull(
//                    NavAction(
//                        title = "id_create_new_account",
//                        icon = "plus_circle",
//                        isMenuEntry = true,
//                        onClick = {
//
//                        }
//                    ).takeIf { !session.isWatchOnly && !greenWallet.isLightning },
//                    NavAction(
//                        title = "id_settings",
//                        icon = "gear_six",
//                        isMenuEntry = true,
//                        onClick = {
//
//                        }
//                    ),
//                    NavAction(
//                        title = "id_log_out",
//                        icon = "sign_out",
//                        isMenuEntry = true,
//                        onClick = {
//                            postEvent(Events.Logout(reason = LogoutReason.USER_ACTION))
//                        }
//                    )
//                )
            )

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
            }.launchIn(this)

            session.getTransactions(account = account, isReset = true, isLoadMore = false)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.Receive -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Receive(
                    greenWallet = greenWallet,
                    accountAsset = accountAsset.value!!
                )))
            }
            is LocalEvents.Send -> {
                if (greenWallet.isWatchOnly || session.accountAssets(account).value.policyAsset != 0L) {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.Send(
                                greenWallet = greenWallet,
                                accountAsset = accountAsset.value!!,
                                isSweep = greenWallet.isWatchOnly
                            )
                        )
                    )
                } else {
                    postSideEffect(SideEffects.OpenDialog())
                }
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

//
//            is LocalEvents.ReconnectFailedNetworks -> {
//                tryFailedNetworks()
//            }
//
//            is LocalEvents.DismissSystemMessage -> {
//                _systemMessage.value = null
//            }
//
//            is LocalEvents.AckSystemMessage -> {
//                ackSystemMessage(event.network, event.message)
//            }
//
//            is LocalEvents.ToggleHideAmounts -> {
//                settingsManager.saveApplicationSettings(
//                    settingsManager.getApplicationSettings().let {
//                        it.copy(hideAmounts = !it.hideAmounts)
//                    }
//                )
//
//                if (settingsManager.appSettings.hideAmounts) {
//                    countly.hideAmount(session)
//                }
//            }
        }
    }

    private fun loadMoreTransactions(){
        logger.i { "Load more transactions" }
        session.getTransactions(account = account, isReset = false, isLoadMore = true)
    }

    private fun enableLightningShortcut() {
        if(account.isLightning) {
            doAsync({
                _enableLightningShortcut()
            }, onSuccess = {

            })
        }
    }

    private fun removeLightningShortcut() {
        if(account.isLightning) {
            doAsync({
                database.deleteLoginCredentials(greenWallet.id, CredentialType.LIGHTNING_MNEMONIC)
            }, onSuccess = {

            })
        }
    }

    companion object: Loggable()
}

class AccountOverviewViewModelPreview() : AccountOverviewViewModelAbstract(greenWallet = previewWallet(), accountAsset = previewAccountAsset()) {
    override val hasLightningShortcut = MutableStateFlow(false)
    override val alerts: StateFlow<List<AlertType>> = MutableStateFlow(listOf())

    override val assets: StateFlow<Map<EnrichedAsset, Long>> = MutableStateFlow(
        mapOf(
            EnrichedAsset.PreviewBTC to 0L,
            EnrichedAsset.PreviewLBTC to 0L,
            EnrichedAsset.PreviewLBTC to 0L
        )
    )

    override val accounts: StateFlow<List<AccountLook>> = MutableStateFlow(listOf(AccountLook.create(previewAccount())))

    override val transactions: StateFlow<List<Transaction>> = MutableStateFlow(listOf())
    override val hasMoreTransactions: StateFlow<Boolean> = MutableStateFlow(false)

    companion object: Loggable(){
        fun create() = AccountOverviewViewModelPreview()
    }
}
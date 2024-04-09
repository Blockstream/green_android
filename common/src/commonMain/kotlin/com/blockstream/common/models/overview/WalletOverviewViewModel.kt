package com.blockstream.common.models.overview

import breez_sdk.HealthCheckStatus
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.toggle
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.looks.account.AccountLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

abstract class WalletOverviewViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "WalletOverview"

    @NativeCoroutinesState
    abstract val alerts: StateFlow<List<AlertType>>

    @NativeCoroutinesState
    abstract val balancePrimary: StateFlow<String>

    @NativeCoroutinesState
    abstract val balanceSecondary: StateFlow<String>

    @NativeCoroutinesState
    abstract val hideAmounts: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isWalletOnboarding: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val assets: StateFlow<List<EnrichedAsset>>

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<AccountLook>>

    @NativeCoroutinesState
    abstract val transactions: StateFlow<List<Transaction>>
}

class WalletOverviewViewModel(greenWallet: GreenWallet) :
    WalletOverviewViewModelAbstract(greenWallet = greenWallet) {
    override fun segmentation(): HashMap<String, Any> =
        countly.sessionSegmentation(session = session)
    
    private val _balancePrimary: MutableStateFlow<String> = MutableStateFlow("")
    override val balancePrimary: StateFlow<String> = _balancePrimary.asStateFlow()

    private val _balanceSecondary: MutableStateFlow<String> = MutableStateFlow("")
    override val balanceSecondary: StateFlow<String> = _balanceSecondary.asStateFlow()

    override val hideAmounts: StateFlow<Boolean> = settingsManager.appSettingsStateFlow.map {
        it.hideAmounts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), settingsManager.appSettings.hideAmounts)

    override val isWalletOnboarding: StateFlow<Boolean> = combine(session.zeroAccounts, session.failedNetworks) { zeroAccounts, failedNetworks ->
        zeroAccounts && failedNetworks.isEmpty()
    }.filter { session.isConnected }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    override val assets: StateFlow<List<EnrichedAsset>> = session.walletAssets
        .filter { session.isConnected }.map {
            it.assets.keys.filter {
                session.hasAssetIcon(it)
            }.map {
                EnrichedAsset.create(session, it)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val accounts: StateFlow<List<AccountLook>> = session.accounts
        .filter { session.isConnected }.map { accounts ->
            accounts.map {
                AccountLook.create(it)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val transactions: StateFlow<List<Transaction>> =
        session.walletTransactions.filter { session.isConnected }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

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

    class LocalEvents {
        object ToggleBalance : Event
        object ToggleHideAmounts : Event
        object Refresh : Event
        object ReconnectFailedNetworks : Event
        class AckSystemMessage(val network: Network, val message: String) : Event
        object DismissSystemMessage : Event
        object Send: Event
        object Receive: Event
    }

    init {
        session.ifConnected {
            _navData.value = NavData(
                title = greenWallet.name,
                subtitle = if(session.isLightningShortcut) "id_lightning_account" else null,
                actions = listOfNotNull(
                    NavAction(
                        title = "id_create_new_account",
                        icon = "plus_circle",
                        isMenuEntry = true,
                        onClick = {

                        }
                    ).takeIf { !session.isWatchOnly && !greenWallet.isLightning },
                    NavAction(
                        title = "id_settings",
                        icon = "gear_six",
                        isMenuEntry = true,
                        onClick = {
                            postEvent(Events.WalletSettings(greenWallet))
                        }
                    ),
                    NavAction(
                        title = "id_log_out",
                        icon = "sign_out",
                        isMenuEntry = true,
                        onClick = {
                            postEvent(Events.Logout(reason = LogoutReason.USER_ACTION))
                        }
                    )
                )
            )

            session.systemMessage.filter { session.isConnected }.onEach {
                _systemMessage.value = if (it.isEmpty()) {
                    null
                } else {
                    AlertType.SystemMessage(it.first().first, it.first().second)
                }
            }.launchIn(viewModelScope.coroutineScope)

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

            combine(session.walletTotalBalance, primaryBalanceInFiat, hideAmounts, session.settings()) { _, _, _, _ ->
                session.isConnected
            }.filter { isConnected ->
                // Prevent from updating on non connected sessions
                isConnected
            }.onEach {
                updateBalance()
            }.launchIn(this)
        }

        bootstrap()
    }

    private suspend fun updateBalance() {
        // Loading
        if (session.walletTotalBalance.value == -1L) {
            _balancePrimary.value = ""
            _balanceSecondary.value = ""
        } else {
            val balance = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
                session = session
            ) ?: ""

            val fiat = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
                session = session,
                denomination = Denomination.fiat(session)
            ) ?: ""

            if (primaryBalanceInFiat.value) {
                _balancePrimary.value = fiat
                _balanceSecondary.value = balance
            } else {
                _balancePrimary.value = balance
                _balanceSecondary.value = fiat
            }
        }
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ToggleBalance -> {
                primaryBalanceInFiat.toggle()
            }

            is LocalEvents.Refresh -> {
                session.refresh()
            }

            is LocalEvents.ReconnectFailedNetworks -> {
                tryFailedNetworks()
            }

            is LocalEvents.DismissSystemMessage -> {
                _systemMessage.value = null
            }

            is LocalEvents.AckSystemMessage -> {
                ackSystemMessage(event.network, event.message)
            }

            is LocalEvents.Send -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Send(greenWallet = greenWallet, accountAsset = session.activeAccount.value!!.accountAsset)))
            }

            is LocalEvents.Receive -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Receive(greenWallet = greenWallet, accountAsset = session.activeAccount.value!!.accountAsset)))
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

    private fun ackSystemMessage(network: Network, message : String){
        doAsync({
            session.ackSystemMessage(network, message)
            session.updateSystemMessage()
        }, onSuccess = {

        })
    }

    private fun tryFailedNetworks() {
        session.tryFailedNetworks(hardwareWalletResolver = session.device?.let { device ->
            DeviceResolver.createIfNeeded(
                device.gdkHardwareWallet,
                // this // TODO enable hw interaction support to GreenViewModel
            )
        })
    }
}

class WalletOverviewViewModelPreview() :
    WalletOverviewViewModelAbstract(greenWallet = previewWallet()) {

    override val isWalletOnboarding: StateFlow<Boolean> = MutableStateFlow(false)

    override val alerts: StateFlow<List<AlertType>> = MutableStateFlow(listOf())

    override val balancePrimary: StateFlow<String> = MutableStateFlow("1.00000 BTC")

    override val balanceSecondary: StateFlow<String> = MutableStateFlow("90.000 USD")

    override val hideAmounts: StateFlow<Boolean> = MutableStateFlow(false)

    override val assets: StateFlow<List<EnrichedAsset>> = MutableStateFlow(
        listOf(
            EnrichedAsset.PreviewBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC,
            EnrichedAsset.PreviewLBTC
        )
    )

    override val accounts: StateFlow<List<AccountLook>> = MutableStateFlow(listOf(AccountLook.create(previewAccount())))

    override val transactions: StateFlow<List<Transaction>> = MutableStateFlow(listOf())

    companion object: Loggable(){
        fun create() = WalletOverviewViewModelPreview()
    }
}
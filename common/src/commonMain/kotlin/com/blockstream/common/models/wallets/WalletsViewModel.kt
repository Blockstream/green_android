package com.blockstream.common.models.wallets

import com.blockstream.common.data.Banner
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class WalletsViewModelAbstract(val isHome: Boolean): GreenViewModel() {
    override fun screenName(): String? = "Home".takeIf { isHome }

    @NativeCoroutinesState
    abstract val isEmptyWallet: StateFlow<Boolean?>

    @NativeCoroutinesState
    abstract val softwareWallets: StateFlow<List<WalletListLook>?>

    @NativeCoroutinesState
    abstract val ephemeralWallets: StateFlow<List<WalletListLook>?>

    @NativeCoroutinesState
    abstract val hardwareWallets: StateFlow<List<WalletListLook>?>

    @NativeCoroutinesState
    abstract val termsOfServiceIsChecked: MutableStateFlow<Boolean>
}

abstract class WalletsViewModel(isHome: Boolean) : WalletsViewModelAbstract(isHome) {

    override val isEmptyWallet: StateFlow<Boolean?> = database.walletsExistsFlow().map {
        !it
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    @NativeCoroutinesState
    override val termsOfServiceIsChecked =
        MutableStateFlow(viewModelScope, settingsManager.isDeviceTermsAccepted())

    override val softwareWallets = combine(
        database.getWalletsFlow(
            credentialType = CredentialType.LIGHTNING_MNEMONIC,
            isHardware = false
        ), sessionManager.connectionChangeEvent
    ) { wallets, _ ->
        wallets
    }.map {
        it.map { greenWallet ->
            WalletListLook.create(greenWallet, sessionManager)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val ephemeralWallets = combine(
        sessionManager.ephemeralWallets,
        sessionManager.connectionChangeEvent
    ) { wallets, _ ->
        wallets
    }.map {
        it.map { greenWallet ->
            WalletListLook.create(greenWallet, sessionManager)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val hardwareWallets = combine(
        database.getWalletsFlow(
            credentialType = CredentialType.LIGHTNING_MNEMONIC,
            isHardware = true
        ),
        sessionManager.hardwareWallets.map { ephemeralWallets ->
            ephemeralWallets
        },
        sessionManager.connectionChangeEvent
    ) { w1, w2, _ ->
        (w1 + w2).distinctBy { it.id }.map { greenWallet ->
            WalletListLook.create(greenWallet, sessionManager)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    class LocalEvents{
        class SelectWallet(val greenWallet: GreenWallet, val isLightningShortcut: Boolean = false): Event
        class RemoveLightningShortcut(val greenWallet: GreenWallet): Event
    }

    init {
        // If you have already agreed, check by default
        viewModelScope.coroutineScope.launch {
            termsOfServiceIsChecked.value =
                settingsManager.isDeviceTermsAccepted() || database.walletsExists()
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is LocalEvents.SelectWallet) {
            val parentWallet = event.greenWallet
            val childWallet: GreenWallet = parentWallet.let { if (event.isLightningShortcut) it.lightningShortcutWallet() else it }
            val session: GdkSession = sessionManager.getWalletSessionOrCreate(childWallet)

            if (session.isConnected) {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(childWallet)))
            } else if (childWallet.isHardware && !event.isLightningShortcut) {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.DeviceScan(
                            greenWallet = childWallet
                        )
                    )
                )
            } else {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Login(
                            greenWallet = parentWallet,
                            isLightningShortcut = event.isLightningShortcut
                        )
                    )
                )
            }
        }else if(event is LocalEvents.RemoveLightningShortcut){
            removeLightningShortcut(event.greenWallet)
        }
    }

    private fun removeLightningShortcut(greenWallet: GreenWallet) {
        doAsync({
            database.deleteLoginCredentials(greenWallet.id, CredentialType.LIGHTNING_MNEMONIC)
        }, onSuccess = {

        })
    }
}

class WalletsViewModelPreview(
    softwareWallets: List<WalletListLook>,
    ephemeralWallets: List<WalletListLook>,
    hardwareWallets: List<WalletListLook>,
) : WalletsViewModelAbstract(isHome = true) {
    override val isEmptyWallet: StateFlow<Boolean?> = MutableStateFlow(viewModelScope, softwareWallets.isEmpty() && ephemeralWallets.isEmpty() && hardwareWallets.isEmpty())

    override val softwareWallets: StateFlow<List<WalletListLook>?> =
        MutableStateFlow(viewModelScope, softwareWallets)
    override val ephemeralWallets: StateFlow<List<WalletListLook>> =
        MutableStateFlow(viewModelScope, ephemeralWallets)
    override val hardwareWallets: StateFlow<List<WalletListLook>> =
        MutableStateFlow(viewModelScope, hardwareWallets)
    override val termsOfServiceIsChecked: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    init {
        banner.value = Banner.preview3
    }
    companion object {
        fun previewEmpty() = WalletsViewModelPreview(
            listOf(), listOf(), listOf()
        )

        fun previewSoftwareOnly() = WalletsViewModelPreview(
            listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false)
            ), listOf(), listOf()
        )

        fun previewHardwareOnly() = WalletsViewModelPreview(
            listOf(),
            listOf(),
            listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false)
            )
        )

        fun previewAll() = WalletsViewModelPreview(
            listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false)
            ), listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = true),
            ), listOf(
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true)
            )
        )
    }
}
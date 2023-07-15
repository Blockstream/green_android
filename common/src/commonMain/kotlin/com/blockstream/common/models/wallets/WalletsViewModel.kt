package com.blockstream.common.models.wallets

import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.events.Event
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.views.wallet.WalletListLook
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

abstract class WalletsViewModelAbstract(val isHome: Boolean): GreenViewModel() {
    override fun screenName(): String? = "Home".takeIf { isHome }

    @NativeCoroutinesState
    abstract val softwareWallets: StateFlow<List<WalletListLook>?>

    @NativeCoroutinesState
    abstract val ephemeralWallets: StateFlow<List<WalletListLook>?>

    @NativeCoroutinesState
    abstract val hardwareWallets: StateFlow<List<WalletListLook>?>
}

abstract class WalletsViewModel(isHome: Boolean) : WalletsViewModelAbstract(isHome) {

    override val softwareWallets = combine(database.getWalletsFlow(credentialType = CredentialType.LIGHTNING_MNEMONIC, isHardware = false), sessionManager.connectionChangeEvent) { wallets, _ ->
        wallets
    }.map {
        it.map { greenWallet ->
            WalletListLook.create(greenWallet, sessionManager)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val ephemeralWallets = combine(sessionManager.ephemeralWallets, sessionManager.connectionChangeEvent) { wallets, _ ->
        wallets
    }.map {
        it.map { greenWallet ->
            WalletListLook.create(greenWallet, sessionManager)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val hardwareWallets = combine(
        database.getWalletsFlow(true),
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
        class SelectWallet(val wallet: GreenWallet, val isLightningShortcut: Boolean = false): Event
    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is LocalEvents.SelectWallet) {
            val parentWallet = event.wallet
            val childWallet: GreenWallet = parentWallet.let { if (event.isLightningShortcut) it.lightningShortcutWallet() else it }
            val session: GdkSession = sessionManager.getWalletSession(childWallet)

            if (session.isConnected) {
                postSideEffect(SideEffects.NavigateTo(WalletDestinations.WalletOverview(childWallet)))
            } else if (childWallet.isHardware) {
                postSideEffect(
                    SideEffects.NavigateTo(
                        WalletDestinations.DeviceScan(
                            wallet = childWallet
                        )
                    )
                )
            } else {
                postSideEffect(
                    SideEffects.NavigateTo(
                        WalletDestinations.WalletLogin(
                            wallet = parentWallet,
                            isLightningShortcut = event.isLightningShortcut
                        )
                    )
                )
            }
        }
    }
}

class WalletsViewModelPreview(
    softwareWallets: List<WalletListLook>,
    ephemeralWallets: List<WalletListLook>,
    hardwareWallets: List<WalletListLook>,
) : WalletsViewModelAbstract(isHome = true) {

    override val softwareWallets: StateFlow<List<WalletListLook>?> =
        MutableStateFlow(viewModelScope, softwareWallets)
    override val ephemeralWallets: StateFlow<List<WalletListLook>> =
        MutableStateFlow(viewModelScope, ephemeralWallets)
    override val hardwareWallets: StateFlow<List<WalletListLook>> =
        MutableStateFlow(viewModelScope, hardwareWallets)

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
                previewWalletListView(isHardware = false)
            ), listOf(
                previewWalletListView(isHardware = false),
                previewWalletListView(isHardware = true),
            ), listOf(
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true),
                previewWalletListView(isHardware = true)
            )
        )
    }
}
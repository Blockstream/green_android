package com.blockstream.common.models.archived

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map


abstract class ArchivedAccountsViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "ArchivedAccounts"

    @NativeCoroutinesState
    abstract val archivedAccounts: StateFlow<List<Account>>
}

class ArchivedAccountsViewModel(greenWallet: GreenWallet) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet) {

    override val archivedAccounts: StateFlow<List<Account>> = session.allAccounts.map {
        it.filter { it.hidden }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    init {
        bootstrap()
    }
}

class ArchivedAccountsViewModelPreview(greenWallet: GreenWallet) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet) {

    override val archivedAccounts: StateFlow<List<Account>> = MutableStateFlow(listOf())

    companion object {
        fun preview() = ArchivedAccountsViewModelPreview(previewWallet(isHardware = false))
    }
}
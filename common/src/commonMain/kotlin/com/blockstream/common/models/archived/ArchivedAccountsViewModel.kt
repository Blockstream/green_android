package com.blockstream.common.models.archived

import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map


abstract class ArchivedAccountsViewModelAbstract(
    greenWallet: GreenWallet,
    val navigateToRoot: Boolean = false
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "ArchivedAccounts"

    @NativeCoroutinesState
    abstract val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>>
}

class ArchivedAccountsViewModel(greenWallet: GreenWallet, navigateToRoot: Boolean = false) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet, navigateToRoot = navigateToRoot) {

    override val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>> =
        session.allAccounts.map {
            DataState.Success(
                it.filter { it.hidden }.map {
                    AccountAssetBalance.create(accountAsset = it.accountAsset, session = session)
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DataState.Loading)

    init {
        _navData.value = NavData(title = "id_archived_accounts", subtitle = greenWallet.name)

        bootstrap()
    }
}

class ArchivedAccountsViewModelPreview(greenWallet: GreenWallet) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet) {

    override val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>> =
        MutableStateFlow(
            DataState.Success(
                listOf(
                    previewAccountAssetBalance(),
                    previewAccountAssetBalance(),
                    previewAccountAssetBalance()
                )
            )
        )

    companion object {
        fun preview() = ArchivedAccountsViewModelPreview(previewWallet(isHardware = false))
    }
}
package com.blockstream.common.models.archived

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_archived_accounts
import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString


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
                it.filter { it.hidden && it.hasHistory(session) }.map {
                    AccountAssetBalance.create(accountAsset = it.accountAsset, session = session)
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DataState.Loading)

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_archived_accounts), subtitle = greenWallet.name)
        }

        archivedAccounts.onEach {
            onProgress.value = it.isLoading()
        }.launchIn(this)

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
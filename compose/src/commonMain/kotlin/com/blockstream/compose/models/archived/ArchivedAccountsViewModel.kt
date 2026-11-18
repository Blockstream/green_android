package com.blockstream.compose.models.archived

import com.blockstream.compose.utils.toStringHolder
import com.blockstream.compose.utils.StringHolder
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_archived_accounts
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAssetBalance
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.DataState
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.domain.account.HasHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class ArchivedAccountsViewModelAbstract(
    greenWallet: GreenWallet,
    val navigateToRoot: Boolean = false
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "ArchivedAccounts"
    abstract val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>>
    abstract val selectedAccounts: StateFlow<Set<Account>>
    
    abstract fun toggleAccountSelection(account: Account)
    abstract fun clearSelection()
    abstract fun unarchiveSelected()
}

class ArchivedAccountsViewModel(greenWallet: GreenWallet, navigateToRoot: Boolean = false) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet, navigateToRoot = navigateToRoot) {

    private val hasHistoryUseCase: HasHistoryUseCase by inject()

    final override val selectedAccounts : StateFlow<Set<Account>>
        field = MutableStateFlow<Set<Account>>(emptySet())

    final override val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>>
        field = MutableStateFlow<DataState<List<AccountAssetBalance>>>(DataState.Loading)


    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_archived_accounts),
            subtitle = greenWallet.name.toStringHolder()
        )

        session.allAccounts.onEach { allAccounts ->
            archivedAccounts.value = DataState.Success(
                allAccounts.filter {
                    it.hidden
                }.filter {
                    hasHistoryUseCase(session = session, wallet = greenWallet, account = it) || !(it.type == AccountType.BIP49_SEGWIT_WRAPPED && it.pointer == 0L) // GDK default account
                }.map {
                    AccountAssetBalance.create(accountAsset = it.accountAsset, session = session)
                }
            )
        }.launchIn(this)

        archivedAccounts.onEach {
            onProgress.value = it.isLoading()
        }.launchIn(this)

        bootstrap()
    }
    
    override fun toggleAccountSelection(account: Account) {
        selectedAccounts.value = if (selectedAccounts.value.contains(account)) {
            selectedAccounts.value - account
        } else {
            selectedAccounts.value + account
        }
    }
    
    override fun clearSelection() {
        selectedAccounts.value = emptySet()
    }
    
    override fun unarchiveSelected() {
        if (selectedAccounts.value.isNotEmpty()) {
            doAsync({
                selectedAccounts.value.forEach { account ->
                    session.updateAccount(account = account, isHidden = false, userInitiated = true)
                }
            }, onSuccess = {
                //val count = _selectedAccounts.value.size
                //val message = getString(Res.string.id_d_accounts_unarchived_successfully, count)
                //postSideEffect(SideEffects.Snackbar(StringHolder.create(message)))
                
                if (navigateToRoot) {
                    postSideEffect(SideEffects.NavigateToRoot())
                }
                clearSelection()
            })
        }
    }
}

class ArchivedAccountsViewModelPreview(greenWallet: GreenWallet) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet) {
    
    override val selectedAccounts: StateFlow<Set<Account>> = MutableStateFlow(emptySet())

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
    
    override fun toggleAccountSelection(account: Account) {}
    override fun clearSelection() {}
    override fun unarchiveSelected() {}

    companion object {
        fun preview() = ArchivedAccountsViewModelPreview(previewWallet(isHardware = false))
    }
}
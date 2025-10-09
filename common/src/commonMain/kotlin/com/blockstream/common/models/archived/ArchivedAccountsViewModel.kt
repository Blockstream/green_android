package com.blockstream.common.models.archived

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_archived_accounts
import blockstream_green.common.generated.resources.id_unarchive
//import blockstream_green.common.generated.resources.id_d_accounts_unarchived_successfully
import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountType
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
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder

abstract class ArchivedAccountsViewModelAbstract(
    greenWallet: GreenWallet,
    val navigateToRoot: Boolean = false
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "ArchivedAccounts"

    @NativeCoroutinesState
    abstract val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>>
    
    @NativeCoroutinesState
    abstract val selectedAccounts: StateFlow<Set<Account>>
    
    abstract fun toggleAccountSelection(account: Account)
    abstract fun clearSelection()
    abstract fun unarchiveSelected()
}

class ArchivedAccountsViewModel(greenWallet: GreenWallet, navigateToRoot: Boolean = false) :
    ArchivedAccountsViewModelAbstract(greenWallet = greenWallet, navigateToRoot = navigateToRoot) {
    
    private val _selectedAccounts = MutableStateFlow<Set<Account>>(emptySet())
    override val selectedAccounts: StateFlow<Set<Account>> = _selectedAccounts

    override val archivedAccounts: StateFlow<DataState<List<AccountAssetBalance>>> =
        session.allAccounts.map { allAccounts ->
            DataState.Success(
                allAccounts.filter {
                    it.hidden
                }.filter {
                    it.hasHistory(session) || !(it.type == AccountType.BIP49_SEGWIT_WRAPPED && it.pointer == 0L) // GDK default account
                }.map {
                    AccountAssetBalance.create(accountAsset = it.accountAsset, session = session)
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DataState.Loading)

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_archived_accounts),
                subtitle = greenWallet.name
            )
        }

        archivedAccounts.onEach {
            onProgress.value = it.isLoading()
        }.launchIn(this)

        bootstrap()
    }
    
    override fun toggleAccountSelection(account: Account) {
        _selectedAccounts.value = if (_selectedAccounts.value.contains(account)) {
            _selectedAccounts.value - account
        } else {
            _selectedAccounts.value + account
        }
    }
    
    override fun clearSelection() {
        _selectedAccounts.value = emptySet()
    }
    
    override fun unarchiveSelected() {
        if (_selectedAccounts.value.isNotEmpty()) {
            doAsync({
                _selectedAccounts.value.forEach { account ->
                    session.updateAccount(account = account, isHidden = false, userInitiated = true)
                }
            }, onSuccess = {
                session.activeAccount.value?.also {
                    setActiveAccount(it)
                }
                
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
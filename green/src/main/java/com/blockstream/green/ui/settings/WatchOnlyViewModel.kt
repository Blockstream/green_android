package com.blockstream.green.ui.settings

import androidx.lifecycle.*
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.*
import com.blockstream.green.ApplicationScope
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.AppKeystore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class WatchOnlyViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    appKeystore: AppKeystore,
    gdkBridge: GdkBridge,
    applicationScope: ApplicationScope,
    @Assisted wallet: Wallet
) : WalletSettingsViewModel(sessionManager, walletRepository, countly, appKeystore, gdkBridge, applicationScope, wallet) {

    private val _outputDescriptorsAccounts = MutableStateFlow<List<Account>>(listOf())
    val outputDescriptorsAccounts get() = _outputDescriptorsAccounts.asSharedFlow()

    private val _extendedPublicKeysAccounts = MutableStateFlow<List<Account>>(listOf())
    val extendedPublicKeysAccounts get() = _extendedPublicKeysAccounts.asSharedFlow()
    init {
        viewModelScope.launch {
            _outputDescriptorsAccounts.value = withContext(context = Dispatchers.IO){
                session.accounts.filter { it.isSinglesig && it.isBitcoin }.map {
                    // Get account return more detailed account
                    session.getAccount(it)
                }
            }

            _extendedPublicKeysAccounts.value = withContext(context = Dispatchers.IO){
                session.accounts.filter { it.isSinglesig && it.isBitcoin }.map {
                    // Get account return more detailed account
                    session.getAccount(it)
                }
            }
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): WatchOnlyViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }

}
package com.blockstream.green.ui.settings

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
open class WatchOnlyViewModel constructor(
    @InjectedParam wallet: GreenWallet
) : WalletSettingsViewModel(wallet) {

    private val _outputDescriptorsAccounts = MutableStateFlow<List<Account>>(listOf())
    val outputDescriptorsAccounts get() = _outputDescriptorsAccounts.asSharedFlow()

    private val _extendedPublicKeysAccounts = MutableStateFlow<List<Account>>(listOf())
    val extendedPublicKeysAccounts get() = _extendedPublicKeysAccounts.asSharedFlow()
    init {
        viewModelScope.coroutineScope.launch {
            _outputDescriptorsAccounts.value = withContext(context = Dispatchers.IO){
                session.accounts.value.filter { it.isSinglesig && it.isBitcoin }.map {
                    // Get account return more detailed account
                    session.getAccount(it)
                }
            }

            _extendedPublicKeysAccounts.value = withContext(context = Dispatchers.IO){
                session.accounts.value.filter { it.isSinglesig && it.isBitcoin }.map {
                    // Get account return more detailed account
                    session.getAccount(it)
                }
            }
        }
    }
}
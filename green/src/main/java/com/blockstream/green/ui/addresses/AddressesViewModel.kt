package com.blockstream.green.ui.addresses

import androidx.lifecycle.*
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Address
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressesViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted account: Account,
) : AbstractAccountWalletViewModel(sessionManager, walletRepository, countly, wallet, account) {

    private val _addressesLiveData: MutableLiveData<List<Address>> = MutableLiveData()
    val addressesLiveData: LiveData<List<Address>> get() = _addressesLiveData

    private val _pagerLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val pagerLiveData: LiveData<Boolean?> get() = _pagerLiveData

    private var lastPointer : Int? = null

    init {
        getPreviousAddresses()
    }

    fun getPreviousAddresses(){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                session.getPreviousAddresses(account, lastPointer)
            }.also { previousAddresses ->
                lastPointer = previousAddresses.lastPointer ?: 0

                _addressesLiveData.value = (_addressesLiveData.value ?: listOf()) + previousAddresses.addresses
                _pagerLiveData.value = previousAddresses.lastPointer != null
            }
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            account: Account,
        ): AddressesViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            account: Account
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, account) as T
            }
        }
    }
}
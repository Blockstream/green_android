package com.blockstream.green.ui.addresses

import androidx.lifecycle.*
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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
        doUserAction({
            session.getPreviousAddresses(account, lastPointer)
        }, onSuccess = { previousAddresses ->
            lastPointer = previousAddresses.lastPointer ?: 0

            _addressesLiveData.value = (_addressesLiveData.value ?: listOf()) + previousAddresses.addresses
            _pagerLiveData.value = previousAddresses.lastPointer != null
        })
    }

    fun signMessage(address: String, message: String, fn: ((signature: String) -> Unit)){
        doUserAction({
            session.signMessage(
                network = network,
                params = SignMessageParams(
                    address = address,
                    message = message
                ),
                hardwareWalletResolver = DeviceResolver.createIfNeeded(
                    session.gdkHwWallet,
                    this
                )
            ).signature
        }, onSuccess = { signature ->
            fn.invoke(signature)
        })
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
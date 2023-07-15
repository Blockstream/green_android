package com.blockstream.green.ui.addresses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SignMessageParams
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class AddressesViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam account: Account,
) : AbstractAccountWalletViewModel(wallet, account) {

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
            session.getPreviousAddresses(accountValue, lastPointer)
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
}
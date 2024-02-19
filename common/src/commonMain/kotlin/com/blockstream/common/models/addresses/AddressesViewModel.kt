package com.blockstream.common.models.addresses

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.looks.account.AddressLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


abstract class AddressesViewModelAbstract(greenWallet: GreenWallet, account: Account) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = account.accountAsset) {
    override fun screenName(): String = "PreviousAddresses"

    @NativeCoroutinesState
    abstract val addresses: StateFlow<List<AddressLook>>

    @NativeCoroutinesState
    abstract val hasMore: StateFlow<Boolean>
}

class AddressesViewModel(greenWallet: GreenWallet, account: Account) :
    AddressesViewModelAbstract(greenWallet = greenWallet, account = account) {

    private val _addresses: MutableStateFlow<List<AddressLook>> = MutableStateFlow(listOf())
    override val addresses: StateFlow<List<AddressLook>> = _addresses.asStateFlow()

    private val _hasMore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var lastPointer : Int? = null

    class LocalEvents{
        object LoadMore: Event
        data class AddressBlockExplorer(val address: String): Event
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is LocalEvents.LoadMore){
            getPreviousAddresses()
        } else if(event is LocalEvents.AddressBlockExplorer){
            postSideEffect(
                SideEffects.OpenBrowser(
                    url = "${
                        account.network.explorerUrl?.replace(
                            "/tx/",
                            "/address/"
                        )
                    }${event.address}"
                )
            )
        }
    }

    init {
        getPreviousAddresses()
        bootstrap()
    }

    private fun getPreviousAddresses(){
        _hasMore.value = false

        doAsync({
            session.getPreviousAddresses(account = account, lastPointer)
        }, onSuccess = { previousAddresses ->
            lastPointer = previousAddresses.lastPointer ?: 0

            _addresses.value = _addresses.value + previousAddresses.addresses.map {
                AddressLook.create(it, account.network)
            }
            _hasMore.value = previousAddresses.lastPointer != null
        })
    }
}

//class AddressesViewModelPreview(greenWallet: GreenWallet) :
//    AddressesViewModelAbstract(greenWallet = greenWallet) {
//
//    override val addresses: StateFlow<List<Address>> = MutableStateFlow(listOf())
//
//    companion object {
//        fun preview() = AddressesViewModelPreview(previewWallet(isHardware = false))
//    }
//}
package com.blockstream.common.models.addresses

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_addresses
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.looks.account.AddressLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString


abstract class AddressesViewModelAbstract(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "PreviousAddresses"

    @NativeCoroutinesState
    abstract val query: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val addresses: StateFlow<List<AddressLook>>

    @NativeCoroutinesState
    abstract val hasMore: StateFlow<Boolean>

    abstract val canSign: Boolean
}

class AddressesViewModel(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    AddressesViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val query: MutableStateFlow<String> = MutableStateFlow("")

    private val _addresses: MutableStateFlow<List<AddressLook>> = MutableStateFlow(listOf())

    override val addresses: StateFlow<List<AddressLook>> =
        combine(query.map { it.lowercase() }, _addresses) { query, addresses ->
            if (query.isBlank()) {
                addresses
            } else {
                addresses.filter {
                    it.address.lowercase().contains(query)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    private val _hasMore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    override val canSign: Boolean = account.network.canSignMessage

    private var lastPointer: Int? = null

    class LocalEvents {
        object LoadMore : Event
        data class AddressBlockExplorer(val address: String) : Event
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_addresses), subtitle = greenWallet.name)
        }
        getPreviousAddresses()

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.LoadMore) {
            getPreviousAddresses()
        } else if (event is LocalEvents.AddressBlockExplorer) {
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

    private fun getPreviousAddresses() {
        _hasMore.value = false

        doAsync({
            session.getPreviousAddresses(account = account, lastPointer = lastPointer)
        }, onSuccess = { previousAddresses ->
            lastPointer = previousAddresses.lastPointer ?: 0

            _addresses.value += previousAddresses.addresses.map {
                AddressLook.create(it, account.network)
            }
            _hasMore.value = previousAddresses.lastPointer != null
        })
    }
}

class AddressesViewModelPreview(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    AddressesViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override val query: MutableStateFlow<String> = MutableStateFlow("")

    override val addresses: StateFlow<List<AddressLook>> = MutableStateFlow((0..200).map {
        AddressLook(
            address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
            index = it.toLong(),
            txCount = "$it",
            canSign = true
        )
    })

    override val hasMore: StateFlow<Boolean> = MutableStateFlow(false)
    override val canSign: Boolean = true

    companion object {
        fun preview() =
            AddressesViewModelPreview(previewWallet(isHardware = false), previewAccountAsset())
    }

}
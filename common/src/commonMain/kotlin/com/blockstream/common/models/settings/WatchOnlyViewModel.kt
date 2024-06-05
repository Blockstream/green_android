package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.looks.wallet.WatchOnlyLook
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString


abstract class WatchOnlyViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "WalletSettingsWatchOnly"

    @NativeCoroutinesState
    abstract val multisigWatchOnly: StateFlow<List<WatchOnlyLook>>

    @NativeCoroutinesState
    abstract val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>>

    @NativeCoroutinesState
    abstract val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>>
}

class WatchOnlyViewModel(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {

    override val multisigWatchOnly: StateFlow<List<WatchOnlyLook>> = combine(
        session.multisigBitcoinWatchOnly,
        session.multisigLiquidWatchOnly
    ) { bitcoin, liquid ->
        listOfNotNull(
            bitcoin?.let { WatchOnlyLook(network = session.bitcoinMultisig, username = it) },
            liquid?.let { WatchOnlyLook(network = session.liquidMultisig, username = it) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    private val _singleSigAccounts = session.accounts.map { accounts ->
        accounts.filter { it.isSinglesig }
    }

    override val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>> =
        _singleSigAccounts.map { accounts ->
            accounts.filter {
                it.extendedPubkey.isNotBlank()
            }.map {
                WatchOnlyLook(account = it, extendedPubkey = it.extendedPubkey)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>> =
        _singleSigAccounts.map { accounts ->
            accounts.filter {
                it.outputDescriptors.isNotBlank()
            }.map {
                WatchOnlyLook(account = it, outputDescriptors = it.outputDescriptors)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_watchonly), subtitle = greenWallet.name)
        }

        bootstrap()
    }
}

class WatchOnlyViewModelPreview(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {
    companion object {
        fun preview() = WatchOnlyViewModelPreview(previewWallet(isHardware = false))
    }

    override val multisigWatchOnly: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(
            viewModelScope, listOf(
                WatchOnlyLook(
                    network = previewNetwork(),
                    username = "username"
                ),
                WatchOnlyLook(
                    network = previewNetwork(isMainnet = false),
                    username = "username_testnet"
                )
            )
        )

    override val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(
            viewModelScope,
            listOf(WatchOnlyLook(account = previewAccount(), extendedPubkey = "xpub6C364rGP9RCtg8FLop5qQG4eqJ4P34wSpypM4Xw1pZea5WC8ZrUtVCcwDGYMeyyCvSUUjzfimRKh2qsiDbxu9RGx999dKRZKyQPEyiqFUFu"))
        )

    override val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(viewModelScope, listOf(WatchOnlyLook(account = previewAccount(), outputDescriptors = "Ypub6f7htZneT3L1PnbFwdsNzApam7MpwUHAFMf8NyeuK2ioojZMT5qQshsVB2q5kCnpkYVyNxo4XKKnofHYotzWzzHXCjiBSfJ71m3EC6vGYym")))
}
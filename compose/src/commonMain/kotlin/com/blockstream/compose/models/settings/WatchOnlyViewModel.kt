package com.blockstream.compose.models.settings

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.compose.extensions.previewAccount
import com.blockstream.compose.extensions.previewNetwork
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.looks.wallet.WatchOnlyLook
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.tryCatchNullSuspend
import com.blockstream.data.gdk.data.Network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class WatchOnlyViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "WalletSettingsWatchOnly"
    abstract val multisigWatchOnly: StateFlow<List<WatchOnlyLook>>
    abstract val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>>
    abstract val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>>
}

class WatchOnlyViewModel(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {

    private val multisigNetworks: List<Network> = session.activeMultisig

    override val multisigWatchOnly: StateFlow<List<WatchOnlyLook>> = if (multisigNetworks.isEmpty()) {
        MutableStateFlow(listOf())
    } else {
        combine(multisigNetworks.map { session.watchOnlyUsername(it) }) { usernames ->
            multisigNetworks.mapIndexed { index, network ->
                WatchOnlyLook(network = network, username = usernames[index])
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())
    }

    private val _singleSigAccounts = session.accounts.map { accounts ->
        accounts.filter { it.isSinglesig && !it.isLightning }
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
            accounts.mapNotNull { account ->
                (account.outputDescriptors?.takeIf { it.isNotBlank() }
                    ?: tryCatchNullSuspend { session.getAccountDescriptors(account) })
                    ?.takeIf { it.isNotBlank() }
                    ?.let { WatchOnlyLook(account = account, outputDescriptors = it) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_watchonly))
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
            listOf(
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
            listOf(
                WatchOnlyLook(
                    account = previewAccount(),
                    extendedPubkey = "xpub6C364rGP9RCtg8FLop5qQG4eqJ4P34wSpypM4Xw1pZea5WC8ZrUtVCcwDGYMeyyCvSUUjzfimRKh2qsiDbxu9RGx999dKRZKyQPEyiqFUFu"
                )
            )
        )

    override val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(
            listOf(
                WatchOnlyLook(
                    account = previewAccount(),
                    outputDescriptors = "Ypub6f7htZneT3L1PnbFwdsNzApam7MpwUHAFMf8NyeuK2ioojZMT5qQshsVB2q5kCnpkYVyNxo4XKKnofHYotzWzzHXCjiBSfJ71m3EC6vGYym"
                )
            )
        )
}

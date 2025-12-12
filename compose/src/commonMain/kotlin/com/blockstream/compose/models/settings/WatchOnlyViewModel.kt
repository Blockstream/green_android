package com.blockstream.compose.models.settings

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.RichWatchOnly
import com.blockstream.common.data.toJson
import com.blockstream.common.data.toLoginCredentials
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.richWatchOnly
import com.blockstream.common.looks.wallet.WatchOnlyLook
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
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
    abstract val richWatchOnly: StateFlow<List<RichWatchOnly>?>
    abstract val multisigWatchOnly: StateFlow<List<WatchOnlyLook>>
    abstract val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>>
    abstract val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>>
}

class WatchOnlyViewModel(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {

    override val richWatchOnly: StateFlow<List<RichWatchOnly>?> = if (appInfo.isDevelopment)
        database.getLoginCredentialsFlow(greenWallet.id).map {
            it.richWatchOnly?.richWatchOnly(greenKeystore) ?: listOf()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf()) else MutableStateFlow(null)

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

    class LocalEvents {
        object CreateRichWatchOnly : Event
        object DeleteRichWatchOnly : Event
    }

    init {
        viewModelScope.launch {
            _navData.value =
                NavData(title = getString(Res.string.id_watchonly))
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.CreateRichWatchOnly) {
            doAsync({
                val rwo = database.getLoginCredential(greenWallet.id, CredentialType.RICH_WATCH_ONLY)?.richWatchOnly(greenKeystore)
                session.updateRichWatchOnly(rwo ?: listOf()).also {
                    database.replaceLoginCredential(
                        it.toLoginCredentials(
                            session = session,
                            greenWallet = greenWallet,
                            greenKeystore = greenKeystore
                        )
                    )
                }

            }, onSuccess = {
                postSideEffect(SideEffects.Dialog(StringHolder.create("DEBUG"), StringHolder.create(it.toJson())))
            })
        } else if (event is LocalEvents.DeleteRichWatchOnly) {
            doAsync({
                database.deleteLoginCredentials(greenWallet.id, CredentialType.RICH_WATCH_ONLY)
            }, onSuccess = {})
        }
    }

    companion object : Loggable()
}

class WatchOnlyViewModelPreview(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {
    companion object {
        fun preview() = WatchOnlyViewModelPreview(previewWallet(isHardware = false))
    }

    override val richWatchOnly: StateFlow<List<RichWatchOnly>> = MutableStateFlow(
        listOf(
            RichWatchOnly("id", "username", "password", "data")
        )
    )

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
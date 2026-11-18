package com.blockstream.compose.models.assetaccounts

import com.blockstream.compose.utils.toStringHolder
import com.blockstream.compose.utils.StringHolder
import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.ifConnectedSuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class AccountDescriptorViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "AccountDescriptor"
    abstract val descriptor: StateFlow<String?>
}

class AccountDescriptorViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : AccountDescriptorViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    final override val descriptor: StateFlow<String?>
        field = MutableStateFlow(null)

    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_watchonly),
            subtitle = account.name.toStringHolder()
        )

        viewModelScope.launch {
            session.ifConnectedSuspend {
                descriptor.value = session.getAccountDescriptors(account)
            }
        }

        bootstrap()
    }
}

class AccountDescriptorViewModelPreview(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : AccountDescriptorViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val descriptor: StateFlow<String> = MutableStateFlow(
        "wpkh([73c5da0a/84'/1'/0']tpubDC8msFGeGuwnKG9Upg7DM2b4DaRqg3CUZa5g8v2SRQ6K4NSkxUgd7HsL2XVWbVm39yBA4LAxysQAm397zwQSQoQgewGiYZqrA9DsP4zbQ1M/0/*)#2e4n992d"
    )

    companion object {
        fun preview() = AccountDescriptorViewModelPreview(
            greenWallet = previewWallet(),
            accountAsset = previewAccountAsset()
        )
    }
}
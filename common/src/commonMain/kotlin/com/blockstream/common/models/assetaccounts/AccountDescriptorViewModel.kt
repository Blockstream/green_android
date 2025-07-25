package com.blockstream.common.models.assetaccounts

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.getString

abstract class AccountDescriptorViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "AccountDescriptor"

    @NativeCoroutinesState
    abstract val descriptor: StateFlow<String>
}

class AccountDescriptorViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : AccountDescriptorViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val descriptor: StateFlow<String> = MutableStateFlow(
        account.outputDescriptors ?: ""
    )

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_watchonly),
                subtitle = account.name
            )
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
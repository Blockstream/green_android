package com.blockstream.common.models.overview

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_my_assets
import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString

abstract class WalletAssetsViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "Assets"

    @NativeCoroutinesState
    abstract val assets: StateFlow<DataState<List<AssetBalance>>>
}

class WalletAssetsViewModel(
    greenWallet: GreenWallet,
) : WalletAssetsViewModelAbstract(
    greenWallet = greenWallet
) {
    override val assets: StateFlow<DataState<List<AssetBalance>>> =
        session.walletAssets.map {
            it.mapSuccess { assets ->
                assets.assets.map {
                    AssetBalance.create(
                        assetId = it.key,
                        balance = it.value,
                        session = session
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            DataState.Loading
        )

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_my_assets), subtitle = greenWallet.name)
        }
        bootstrap()
    }
}

class WalletAssetsViewModelPreview() :
    WalletAssetsViewModelAbstract(greenWallet = previewWallet()) {

    companion object : Loggable() {
        fun create() = WalletAssetsViewModelPreview()
    }

    override val assets: StateFlow<DataState<List<AssetBalance>>> = MutableStateFlow(
        DataState.successOrEmpty(
            listOf(
                previewAssetBalance()
            )
        )
    )
}
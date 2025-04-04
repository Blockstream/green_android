package com.blockstream.common.models.overview

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_my_assets
import com.blockstream.common.data.DataState
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.Loggable
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
        session.walletAssets.map { assets ->
            session.ifConnected {
                DataState.Success(assets.assets.map {
                    AssetBalance.create(
                        assetId = it.key,
                        balance = it.value,
                        session = session
                    )
                })
            } ?: DataState.Empty
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
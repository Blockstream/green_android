package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_my_assets
import com.blockstream.data.data.DataState
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.extensions.previewAssetBalance
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class WalletAssetsViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "Assets"
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
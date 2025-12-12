package com.blockstream.compose.models.receive

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_select_asset
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetList
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.domain.boltz.BoltzUseCase
import com.blockstream.domain.receive.ReceiveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class ReceiveChooseAssetViewModelAbstract(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "ReceiveChooseAsset"

    abstract val assets: StateFlow<List<EnrichedAsset>>
    abstract val isSwapsEnabled: StateFlow<Boolean>

    abstract fun selectAsset(asset: EnrichedAsset)
}

class ReceiveChooseAssetViewModel(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null
) : ReceiveChooseAssetViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    internal val receiveUseCase: ReceiveUseCase by inject()
    internal val boltzUseCase: BoltzUseCase by inject()

    private val _assets: MutableStateFlow<List<EnrichedAsset>> = MutableStateFlow(listOf())
    override val assets: StateFlow<List<EnrichedAsset>> = _assets

    private val _isSwapsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isSwapsEnabled: StateFlow<Boolean> = _isSwapsEnabled

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_receive),
                subtitle = getString(Res.string.id_select_asset),
            )
        }

        doAsync({
            _assets.value = receiveUseCase.getReceiveAssetsUseCase(session = session)
            _isSwapsEnabled.value = boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet)
        })
    }

    override fun selectAsset(asset: EnrichedAsset) {
        doAsync({
            val accounts = receiveUseCase.getReceiveAccountsUseCase(session = session, asset = asset)

            if (accounts.isEmpty()) {
                throw Exception("id_insufficient_funds")
            }

            if (accounts.size == 1) {
                SideEffects.NavigateTo(
                    NavigateDestinations.Receive(
                        greenWallet = greenWallet,
                        accountAsset = accounts.first()
                    )
                )
            } else {
                SideEffects.NavigateTo(
                    NavigateDestinations.ReceiveChooseAccount(
                        greenWallet = greenWallet,
                        accounts = AccountAssetList(accounts)
                    )
                )
            }
        }, onSuccess = {
            postSideEffect(it)
        })
    }
}

class ReceiveChooseAssetViewModelPreview(greenWallet: GreenWallet) :
    ReceiveChooseAssetViewModelAbstract(greenWallet = greenWallet) {
    override val assets = MutableStateFlow(listOf(EnrichedAsset.PreviewBTC, EnrichedAsset.PreviewLBTC))
    override val isSwapsEnabled = MutableStateFlow(true)

    override fun selectAsset(asset: EnrichedAsset) {

    }

    companion object {
        fun preview() = ReceiveChooseAssetViewModelPreview(previewWallet())
    }
}

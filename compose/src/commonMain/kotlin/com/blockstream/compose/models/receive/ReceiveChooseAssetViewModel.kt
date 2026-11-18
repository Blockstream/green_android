package com.blockstream.compose.models.receive

import com.blockstream.compose.utils.StringHolder
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_select_asset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetList
import com.blockstream.data.swap.SwapAsset
import com.blockstream.domain.receive.GetReceiveAssetsUseCase
import com.blockstream.domain.receive.ReceiveUseCase
import com.blockstream.domain.swap.SwapDirection
import com.blockstream.domain.swap.SwapUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

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

    internal val getReceiveAssetsUseCase: GetReceiveAssetsUseCase by inject {
        parametersOf(session)
    }

    internal val boltzUseCase: SwapUseCase by inject()

    private val _assets: MutableStateFlow<List<EnrichedAsset>> = MutableStateFlow(listOf())
    override val assets: StateFlow<List<EnrichedAsset>> = _assets

    private val _isSwapsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isSwapsEnabled: StateFlow<Boolean> = _isSwapsEnabled

    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_receive),
            subtitle = StringHolder(stringResource = Res.string.id_select_asset),
        )

        doAsync({
            _assets.value = getReceiveAssetsUseCase()
            _isSwapsEnabled.value = boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet) &&
                    boltzUseCase.isSwapDirectionAvailableUseCase.isEnabled(
                        SwapDirection(from = SwapAsset.Lightning, to = SwapAsset.Liquid)
                    )
        })
    }

    override fun selectAsset(asset: EnrichedAsset) {
        doAsync({
            val accounts = receiveUseCase.getReceiveAccountsUseCase(session = session, asset = asset)

            if (accounts.isEmpty()) {
                throw Exception("id_no_available_accounts")
            }

            val account = accountAsset.value?.account?.let { account ->
                accounts.find {
                    it.account == account
                }
            } ?: accounts.firstOrNull()?.takeIf {
                accounts.size == 1
            }

            if (account != null) {
                SideEffects.NavigateTo(
                    NavigateDestinations.Receive(
                        greenWallet = greenWallet,
                        accountAsset = account
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

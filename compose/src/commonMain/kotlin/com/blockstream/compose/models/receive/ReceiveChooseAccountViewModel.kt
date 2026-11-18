package com.blockstream.compose.models.receive

import com.blockstream.compose.utils.StringHolder
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_select_account
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects

abstract class ReceiveChooseAccountViewModelAbstract(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "ReceiveChooseAccount"

    abstract val accounts: List<AccountAsset>

    abstract fun selectAccount(account: AccountAsset)
}

class ReceiveChooseAccountViewModel(
    greenWallet: GreenWallet,
    override val accounts: List<AccountAsset>,
    accountAssetOrNull: AccountAsset? = null
) : ReceiveChooseAccountViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_receive),
            subtitle = StringHolder(stringResource = Res.string.id_select_account),
        )
    }

    override fun selectAccount(account: AccountAsset) {
        doAsync({
            SideEffects.NavigateTo(
                NavigateDestinations.Receive(
                    greenWallet = greenWallet,
                    accountAsset = account
                )
            )
        }, onSuccess = {
            postSideEffect(it)
        })
    }
}

class ReceiveChooseAccountViewModelPreview(greenWallet: GreenWallet) :
    ReceiveChooseAccountViewModelAbstract(greenWallet = greenWallet) {

    override val accounts = listOf(previewAccountAsset())

    override fun selectAccount(account: AccountAsset) {

    }

    companion object {
        fun preview() = ReceiveChooseAccountViewModelPreview(previewWallet())
    }
}

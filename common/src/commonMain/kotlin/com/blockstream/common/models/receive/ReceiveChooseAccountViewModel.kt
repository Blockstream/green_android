package com.blockstream.common.models.receive

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_select_account
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.ui.navigation.NavData
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import org.jetbrains.compose.resources.getString

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
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_receive),
                subtitle = getString(Res.string.id_select_account),
            )
        }
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

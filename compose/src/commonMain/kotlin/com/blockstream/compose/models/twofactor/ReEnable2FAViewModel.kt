package com.blockstream.compose.models.twofactor

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_insufficient_lbtc_for_fees
import blockstream_green.common.generated.resources.id_insufficient_lbtc_to_send_a
import blockstream_green.common.generated.resources.id_reenable_2fa
import com.blockstream.data.Urls
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.extensions.previewAccount
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.Account
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class ReEnable2FAViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "ReEnable2FA"
    abstract val accounts: StateFlow<List<Account>>
}

class ReEnable2FAViewModel(greenWallet: GreenWallet) :
    ReEnable2FAViewModelAbstract(greenWallet = greenWallet) {

    override val accounts: StateFlow<List<Account>> = session.expired2FA

    class LocalEvents {
        data class SelectAccount(val account: Account) : Event
        object LearnMore : Events.OpenBrowser(Urls.HELP_2FA_PROTECTION)
    }

    init {

        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_reenable_2fa), subtitle = greenWallet.name)
        }
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SelectAccount) {
            if (session.accountAssets(event.account).value.policyAsset == 0L) {
                postSideEffect(
                    SideEffects.Dialog(
                        title = StringHolder.create(Res.string.id_insufficient_lbtc_for_fees),
                        message = StringHolder.create(Res.string.id_insufficient_lbtc_to_send_a)
                    )
                )
            } else {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Redeposit(
                            greenWallet = greenWallet,
                            accountAsset = event.account.accountAsset,
                            isRedeposit2FA = true
                        )
                    )
                )
            }
        }
    }
}

class ReEnable2FAViewModelPreview(
    greenWallet: GreenWallet
) : ReEnable2FAViewModelAbstract(greenWallet = greenWallet) {

    override val accounts: StateFlow<List<Account>> =
        MutableStateFlow(listOf(previewAccount(), previewAccount(), previewAccount()))

    companion object {
        fun preview() = ReEnable2FAViewModelPreview(
            previewWallet()
        )
    }

}
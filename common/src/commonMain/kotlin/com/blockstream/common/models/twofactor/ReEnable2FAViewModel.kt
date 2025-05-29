package com.blockstream.common.models.twofactor

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_insufficient_lbtc_for_fees
import blockstream_green.common.generated.resources.id_insufficient_lbtc_to_send_a
import blockstream_green.common.generated.resources.id_reenable_2fa
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.getString

abstract class ReEnable2FAViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "ReEnable2FA"

    @NativeCoroutinesState
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
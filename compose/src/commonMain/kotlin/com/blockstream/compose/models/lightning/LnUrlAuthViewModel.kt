package com.blockstream.compose.models.lightning

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authentication_successful
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SupportData
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.lightning.LnUrlAuthData
import com.blockstream.data.lightning.LnUrlAuthOutcome
import kotlinx.coroutines.launch

abstract class LnUrlAuthViewModelAbstract(
    greenWallet: GreenWallet,
    val requestData: LnUrlAuthData
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "LNURLAuth"
}

class LnUrlAuthViewModel(greenWallet: GreenWallet, requestData: LnUrlAuthData) :
    LnUrlAuthViewModelAbstract(greenWallet = greenWallet, requestData = requestData) {

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = "LNURL Auth", subtitle = greenWallet.name)
        }
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            auth()
        }
    }

    private fun auth() {
        doAsync({
            session.lightningSdk.authLnUrl(requestData = requestData).also {
                if (it is LnUrlAuthOutcome.Error) {
                    throw Exception(it.reason)
                }
            }
        }, postAction = {}, onSuccess = {
            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_authentication_successful)))
            postSideEffect(SideEffects.NavigateBack())
        }, onError = {
            postSideEffect(
                SideEffects.NavigateBack(
                    error = it,
                    supportData = SupportData.create(
                        throwable = it,
                        network = session.lightning,
                        session = session
                    )
                )
            )
        })
    }

    override fun errorReport(exception: Throwable): SupportData {
        return SupportData.create(
            throwable = exception,
            network = session.lightning,
            session = session
        )
    }
}

class LnUrlAuthViewModelPreview(greenWallet: GreenWallet) :
    LnUrlAuthViewModelAbstract(
        greenWallet = greenWallet,
        requestData = LnUrlAuthData(k1 = "k1", domain = "domain", url = "url", action = "action")
    ) {
    companion object {
        fun preview(): LnUrlAuthViewModelPreview {
            return LnUrlAuthViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }
}

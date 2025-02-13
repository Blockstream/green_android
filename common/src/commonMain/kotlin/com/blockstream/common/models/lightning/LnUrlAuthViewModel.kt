package com.blockstream.common.models.lightning

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authentication_successful
import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlCallbackStatus
import com.blockstream.common.data.SupportData
import com.blockstream.common.data.GreenWallet
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.rickclephas.kmp.observableviewmodel.launch

abstract class LnUrlAuthViewModelAbstract(
    greenWallet: GreenWallet,
    val requestData: LnUrlAuthRequestData
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "LNURLAuth"
}

class LnUrlAuthViewModel(greenWallet: GreenWallet, requestData: LnUrlAuthRequestData) :
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
                if (it is LnUrlCallbackStatus.ErrorStatus) {
                    throw Exception(it.data.reason)
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
        requestData = LnUrlAuthRequestData("k1", "domain", "url", "action")
    ) {
    companion object {
        fun preview(): LnUrlAuthViewModelPreview {
            return LnUrlAuthViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }
}
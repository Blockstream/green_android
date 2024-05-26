package com.blockstream.common.models.lightning

import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlCallbackStatus
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects

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
        _navData.value = NavData(title = "LNURL Auth", subtitle = greenWallet.name)
        bootstrap()
    }

    override fun handleEvent(event: Event) {
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
            postSideEffect(SideEffects.Snackbar("id_authentication_successful"))
            postSideEffect(SideEffects.NavigateBack())
        }, onError = {
            postSideEffect(
                SideEffects.NavigateBack(
                    error = it,
                    errorReport = ErrorReport.create(
                        throwable = it,
                        network = session.lightning,
                        session = session
                    )
                )
            )
        })
    }

    override fun errorReport(exception: Throwable): ErrorReport {
        return ErrorReport.create(
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
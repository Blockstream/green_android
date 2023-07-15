package com.blockstream.green.ui.lightning

import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlCallbackStatus
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class LnUrlAuthViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam val accountAsset: AccountAsset,
    @InjectedParam val requestData: LnUrlAuthRequestData,
) : AbstractAccountWalletViewModel(
    wallet,
    accountAsset.account
) {

    fun auth() {
        doUserAction({
            session.lightningSdk.authLnUrl(requestData = requestData).also {
                if (it is LnUrlCallbackStatus.ErrorStatus) {
                    throw Exception(it.data.reason)
                }
            }
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar("id_authentication_successful"))
            postSideEffect(SideEffects.NavigateBack())
        })
    }
}

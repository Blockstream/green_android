package com.blockstream.green.ui.lightning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import breez_sdk.LnUrlCallbackStatus
import breez_sdk.LnUrlAuthRequestData
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


class LnUrlAuthViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted val accountAsset: AccountAsset,
    @Assisted val requestData: LnUrlAuthRequestData,
) : AbstractAccountWalletViewModel(
    sessionManager,
    walletRepository,
    countly,
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
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            accountAsset: AccountAsset,
            requestData: LnUrlAuthRequestData
        ): LnUrlAuthViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            accountAsset: AccountAsset,
            requestData: LnUrlAuthRequestData
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return assistedFactory.create(
                        wallet,
                        accountAsset,
                        requestData
                    ) as T
                }
            }
    }
}

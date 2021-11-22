package com.blockstream.green.ui.wallet;

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.params.AddressParams
import com.blockstream.gdk.params.SweepParams
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenapi.Session
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy


class SweepViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    val privateKey = MutableLiveData<String>()

    fun sweep() {
        session.observable {
            val params = SweepParams(
                feeRate = session.getFeeEstimates().fees.getOrNull(0) ?: session.network.defaultFee,
                privateKey = privateKey.value?.trim() ?: "",
                passphrase = "",
                addressees = listOf(AddressParams(address = session.getReceiveAddress(wallet.activeAccount).address)),
                subAccount = wallet.activeAccount
            )

            it.createTransaction(params).let { tx ->
                if (!tx.error.isNullOrBlank()) {
                    throw Exception(tx.error)
                }

                tx.toObjectNode()
            }

        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                Session.getSession().pendingTransaction = it
                onEvent.postValue(ConsumableEvent(NavigateEvent.Navigate))
            },
            onError = {
                it.printStackTrace()
                onError.postValue(ConsumableEvent(it))
            }
        )
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): SweepViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}
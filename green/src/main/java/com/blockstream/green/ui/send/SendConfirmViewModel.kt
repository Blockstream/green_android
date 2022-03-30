package com.blockstream.green.ui.send;

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.TwoFactorResolver
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging


class SendConfirmViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    val editableNote = MutableLiveData(session.pendingTransaction?.second?.memo ?: "") // bump memo
    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    fun broadcastTransaction(twoFactorResolver: TwoFactorResolver) {
        session.observable {

            // Create transaction with memo
            val params = it.pendingTransaction!!.first.copy(
                memo = (editableNote.value ?: "").trim()
            )

            val transaction = it.createTransaction(params)

            if(session.hasDevice && session.device?.isLedger == false){
                deviceAddressValidationEvent.postValue(ConsumableEvent(null))
            }

            // Sign transaction
            val signedTransaction = it.signTransaction(transaction)

            // Send or Broadcast
            if (signedTransaction.isSweep) {
                 it.broadcastTransaction(signedTransaction.transaction ?: "")
            } else {
                 it.sendTransaction(signedTransaction, twoFactorResolver = twoFactorResolver).txHash!!
            }

        }.doOnSubscribe {
            onProgress.postValue(true)
        }
        .subscribeBy(
            onSuccess = {
                deviceAddressValidationEvent.value = ConsumableEvent(true)
                onEvent.postValue(ConsumableEvent(NavigateEvent.Navigate))
                session.pendingTransaction = null // clear pending transaction
            },
            onError = {
                it.printStackTrace()
                deviceAddressValidationEvent.value = ConsumableEvent(false)
                onError.postValue(ConsumableEvent(it))
                // Set progress to false only onError as expected behavior for success is to navigate away and we want to avoid animation glitches
                onProgress.postValue(false)
            }
        )
    }


    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): SendConfirmViewModel
    }

    companion object : KLogging() {
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
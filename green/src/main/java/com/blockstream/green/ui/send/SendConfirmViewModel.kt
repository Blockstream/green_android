package com.blockstream.green.ui.send;

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.Account
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.data.TransactionSegmentation
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.gdk.TwoFactorResolver
import com.blockstream.green.ui.bottomsheets.ITransactionNote
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.utils.ConsumableEvent

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mu.KLogging


class SendConfirmViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted account: Account,
    @Assisted val transactionSegmentation: TransactionSegmentation
) : AbstractAccountWalletViewModel(sessionManager, walletRepository, countly, wallet, account), ITransactionNote {

    val transactionNoteLiveData = MutableLiveData(session.pendingTransaction?.second?.memo ?: "")
    val transactionNote get() = (transactionNoteLiveData.value ?: "").trim()

    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    override fun saveNote(note: String) {
        transactionNoteLiveData.value = note
    }

    fun broadcastTransaction(twoFactorResolver: TwoFactorResolver) {
        doUserAction({
            // Create transaction with memo
            val params = session.pendingTransaction!!.first.copy(
                memo = transactionNote
            )
            var transaction = session.pendingTransaction!!.second
            val isSwap = transaction.signWith.containsAll(listOf("user", "green-backend"))

            if (!isSwap) {
                transaction = session.createTransaction(network, params).also { tx ->
                    // Update pending transaction so that VerifyTransactionBottomSheet can get the actual tx to be broadcast
                    session.pendingTransaction = params to tx
                }
            }

            if (session.isHardwareWallet) {
                deviceAddressValidationEvent.postValue(ConsumableEvent(null))
            }

            // Sign transaction
            val signedTransaction = session.signTransaction(network, transaction)

            // Send or Broadcast
            if (signedTransaction.isSweep || isSwap) {
                session.broadcastTransaction(network, signedTransaction.transaction ?: "")
            } else {
                session.sendTransaction(
                    network,
                    signedTransaction,
                    twoFactorResolver = twoFactorResolver
                ).txHash!!
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            deviceAddressValidationEvent.value = ConsumableEvent(true)
            val isSendAll = session.pendingTransaction?.first?.sendAll ?: false
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(isSendAll)))
            countly.endSendTransaction(
                session = session,
                account = account,
                transactionSegmentation = transactionSegmentation,
                withMemo = transactionNote.isNotBlank()
            )
            session.pendingTransaction = null // clear pending transaction
        }, onError = {
            onError.postValue(ConsumableEvent(it))
            deviceAddressValidationEvent.value = ConsumableEvent(false)
            countly.failedTransaction(session = session, error = it)
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            account: Account,
            transactionSegmentation: TransactionSegmentation
        ): SendConfirmViewModel
    }

    companion object : KLogging() {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            account: Account,
            transactionSegmentation: TransactionSegmentation
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, account, transactionSegmentation) as T
            }
        }
    }
}

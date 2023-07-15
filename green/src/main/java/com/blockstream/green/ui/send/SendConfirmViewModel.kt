package com.blockstream.green.ui.send;

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.SendTransactionSuccess
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.ui.bottomsheets.INote
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam


@KoinViewModel
class SendConfirmViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam account: Account,
    @InjectedParam val transactionSegmentation: TransactionSegmentation
) : AbstractAccountWalletViewModel(wallet, account), INote {

    val transactionNoteLiveData = MutableLiveData(session.pendingTransaction?.second?.memo ?: "")
    val transactionNote get() = (transactionNoteLiveData.value ?: "").trim()

    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    override fun saveNote(note: String) {
        transactionNoteLiveData.value = note
    }

    fun signTransaction(broadcast: Boolean, twoFactorResolver: TwoFactorResolver) {
        doUserAction({
            countly.startSendTransaction()
            countly.startFailedTransaction()

            // Create transaction with memo
            val params = session.pendingTransaction!!.first.copy(
                memo = transactionNote
            )
            var transaction = session.pendingTransaction!!.second
            val isSwap = transaction.isSwap()

            if (!isSwap) {
                transaction = session.createTransaction(network, params).also { tx ->
                    // Update pending transaction so that VerifyTransactionBottomSheet can get the actual tx to be broadcast
                    session.pendingTransaction = params to tx
                }
            }

            // If liquid, blind the transaction before signing
            if (network.isLiquid) {
                transaction = session.blindTransaction(network, transaction)
            }

            if (session.isHardwareWallet) {
                deviceAddressValidationEvent.postValue(ConsumableEvent(null))
            }

            // Sign transaction
            val signedTransaction = session.signTransaction(network, transaction)

            // Send or Broadcast
            if (broadcast) {
                if (signedTransaction.isSweep() || isSwap) {
                    session.broadcastTransaction(network, signedTransaction.transaction ?: "")
                } else {
                    session.sendTransaction(
                        network = network,
                        signedTransaction = signedTransaction,
                        twoFactorResolver = twoFactorResolver
                    )
                }
            } else {
                SendTransactionSuccess(signedTransaction = signedTransaction.transaction ?: "")
            }
        }, postAction = {
            onProgressAndroid.value = it == null
        }, onSuccess = {
            deviceAddressValidationEvent.value = ConsumableEvent(true)

            if(it.signedTransaction == null){
                session.pendingTransaction = null // clear pending transaction
                countly.endSendTransaction(
                    session = session,
                    account = accountValue,
                    transactionSegmentation = transactionSegmentation,
                    withMemo = transactionNote.isNotBlank()
                )
                postSideEffect(SideEffects.Navigate(it))
            }else{
                onProgressAndroid.value = false
                postSideEffect(SideEffects.Success(it))
            }
        }, onError = {
            onError.postValue(ConsumableEvent(it))
            deviceAddressValidationEvent.value = ConsumableEvent(false)
            countly.failedTransaction(session = session, account = accountValue, transactionSegmentation = transactionSegmentation, error = it)
        })
    }
}

package com.blockstream.green.ui.send;

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.ExceptionWithErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.SendTransactionSuccess
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.ui.bottomsheets.INote
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam


@KoinViewModel
class SendConfirmViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam account: Account,
    @InjectedParam val transactionSegmentation: TransactionSegmentation
) : GreenViewModel(wallet, account.accountAsset), INote {

    val transactionNoteLiveData = MutableLiveData(session.pendingTransaction?.second?.memo ?: "")
    val transactionNote get() = (transactionNoteLiveData.value ?: "").trim()

    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    override fun saveNote(note: String) {
        transactionNoteLiveData.value = note
    }

    fun signTransaction(broadcast: Boolean, twoFactorResolver: TwoFactorResolver) {
        doAsync({
            countly.startSendTransaction()
            countly.startFailedTransaction()

            // Create transaction with memo
            val params = session.pendingTransaction!!.first.copy(
                memo = transactionNote
            )
            var transaction = session.pendingTransaction!!.second
            val isSwap = transaction.isSwap()

            if (!isSwap) {
                transaction = session.createTransaction(account.network, params).also { tx ->
                    // Update pending transaction so that VerifyTransactionBottomSheet can get the actual tx to be broadcast
                    session.pendingTransaction = params to tx
                }
            }

            // If liquid, blind the transaction before signing
            if (account.network.isLiquid) {
                transaction = session.blindTransaction(account.network, transaction)
            }

            if (session.isHardwareWallet && !account.network.isLightning) {
                deviceAddressValidationEvent.postValue(ConsumableEvent(null))
            }

            // Sign transaction
            val signedTransaction = session.signTransaction(account.network, transaction)

            // Send or Broadcast
            if (broadcast) {
                if (signedTransaction.isSweep() || isSwap) {
                    session.broadcastTransaction(account.network, signedTransaction.transaction ?: "")
                } else {
                    session.sendTransaction(
                        account = account,
                        signedTransaction = signedTransaction,
                        twoFactorResolver = twoFactorResolver
                    )
                }
            } else {
                SendTransactionSuccess(signedTransaction = signedTransaction.transaction ?: "")
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            deviceAddressValidationEvent.value = ConsumableEvent(true)

            if(it.signedTransaction == null){
                session.pendingTransaction = null // clear pending transaction
                countly.endSendTransaction(
                    session = session,
                    account = account,
                    transactionSegmentation = transactionSegmentation,
                    withMemo = transactionNote.isNotBlank()
                )

                postSideEffect(SideEffects.Navigate(it))
            }else{
                onProgress.value = false
                postSideEffect(SideEffects.Success(it))
            }
        }, onError = {

            when {
                // If the error is the Anti-Exfil validation violation we show that prominently.
                // Otherwise show a toast of the error text.
                it.message == "id_signature_validation_failed_if" -> {
                    postSideEffect(SideEffects.ErrorDialog(it, errorReport = ErrorReport.create(throwable = it, network = account.network, session = session)))
                }
                it.message == "id_transaction_already_confirmed" -> {
                    postSideEffect(SideEffects.Snackbar("id_transaction_already_confirmed"))
                    postSideEffect(SideEffects.NavigateToRoot)
                }
                it.message != "id_action_canceled" -> {
                    postSideEffect(
                        SideEffects.ErrorDialog(
                            it, errorReport = (it as? ExceptionWithErrorReport)?.errorReport
                                ?: ErrorReport.create(
                                    throwable = it,
                                    network = account.network,
                                    session = session
                                )
                        )
                    )
                }
            }

            postSideEffect(SideEffects.ErrorDialog(it))

            deviceAddressValidationEvent.value = ConsumableEvent(false)
            countly.failedTransaction(session = session, account = account, transactionSegmentation = transactionSegmentation, error = it)
        })
    }
}

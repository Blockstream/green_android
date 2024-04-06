package com.blockstream.common.models.send

import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.ExceptionWithErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.SendTransactionSuccess
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow


abstract class SendConfirmViewModelAbstract(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "SendConfirm"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.accountSegmentation(
            session = session,
            account = account
        )
    }

    @NativeCoroutinesState
    abstract val note: MutableStateFlow<String>
}

class SendConfirmViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    private val transactionSegmentation: TransactionSegmentation
) :
    SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val note: MutableStateFlow<String> =
        MutableStateFlow(session.pendingTransaction?.second?.memo ?: "")

    class LocalEvents {
        data class SetNote(val note: String) : Event
        data class SignTransaction(
            val broadcastTransaction: Boolean = true,
            val twoFactorResolver: TwoFactorResolver
        ) : Event
    }

    class LocalSideEffects {
        object DeviceAddressValidation: SideEffect
    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SignTransaction) {
            signTransaction(event.broadcastTransaction, event.twoFactorResolver)
        } else if (event is LocalEvents.SetNote) {
            note.value = event.note
        }
    }

    private fun signTransaction(broadcast: Boolean, twoFactorResolver: TwoFactorResolver) {
        doAsync({
            countly.startSendTransaction()
            countly.startFailedTransaction()

            // Create transaction with memo
            val params = session.pendingTransaction!!.first.copy(
                memo = note.value.trim()
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
                // deviceAddressValidationEvent.postValue(ConsumableEvent(null))
                postSideEffect(LocalSideEffects.DeviceAddressValidation)
            }

            // Sign transaction
            val signedTransaction = session.signTransaction(account.network, transaction)

            // Send or Broadcast
            if (broadcast) {
                if (signedTransaction.isSweep() || isSwap) {
                    session.broadcastTransaction(
                        account.network,
                        signedTransaction.transaction ?: ""
                    )
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
            // Dismiss Verify Transaction Dialog
            // deviceAddressValidationEvent.value = ConsumableEvent(true)
            postSideEffect(SideEffects.Dismiss)

            if (it.signedTransaction == null) {
                session.pendingTransaction = null // clear pending transaction
                countly.endSendTransaction(
                    session = session,
                    account = account,
                    transactionSegmentation = transactionSegmentation,
                    withMemo = note.value.isNotBlank()
                )

                postSideEffect(SideEffects.Navigate(it))
            } else {
                onProgress.value = false
                postSideEffect(SideEffects.Success(it))
            }
        }, onError = {
            // Dismiss Verify Transaction Dialog
            postSideEffect(SideEffects.Dismiss)
            // deviceAddressValidationEvent.value = ConsumableEvent(false)

            when {
                // If the error is the Anti-Exfil validation violation we show that prominently.
                // Otherwise show a toast of the error text.
                it.message == "id_signature_validation_failed_if" -> {
                    postSideEffect(
                        SideEffects.ErrorDialog(
                            it,
                            errorReport = ErrorReport.create(
                                throwable = it,
                                network = account.network,
                                session = session
                            )
                        )
                    )
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
            countly.failedTransaction(
                session = session,
                account = account,
                transactionSegmentation = transactionSegmentation,
                error = it
            )
        })
    }
}

class SendConfirmViewModelPreview(greenWallet: GreenWallet) :
    SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = previewAccountAsset()) {

    override val note: MutableStateFlow<String> = MutableStateFlow("")

    companion object {
        fun preview() = SendConfirmViewModelPreview(previewWallet())
    }
}
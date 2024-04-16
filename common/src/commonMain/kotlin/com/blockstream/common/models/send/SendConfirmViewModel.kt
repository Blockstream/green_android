package com.blockstream.common.models.send

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.Banner
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.ExceptionWithErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.SendTransactionSuccess
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


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

    @NativeCoroutinesState
    abstract val transactionConfirmLook: StateFlow<TransactionConfirmLook?>
}

class SendConfirmViewModel constructor(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    denomination: Denomination?
) :
    SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val note: MutableStateFlow<String> =
        MutableStateFlow(session.pendingTransaction?.second?.memo ?: "")

    private val _transactionConfirmLook: MutableStateFlow<TransactionConfirmLook?> =
        MutableStateFlow(null)

    override val transactionConfirmLook: StateFlow<TransactionConfirmLook?> =
        _transactionConfirmLook.asStateFlow()

    class LocalEvents {
        object Note : Event
        data class SetNote(val note: String) : Event
        data class SignTransaction(
            val broadcastTransaction: Boolean = true,
            val twoFactorResolver: TwoFactorResolver? = null
        ) : Event
    }

    class LocalSideEffects {
        data class Note(val note: String) : SideEffect
        data class DeviceAddressValidation(val transactionConfirmLook: TransactionConfirmLook) : SideEffect
    }

    init {
        _navData.value = NavData(
            title = "id_review", subtitle = greenWallet.name,
            onBackPressed = {
                !(onProgress.value)
            },
            actions = listOfNotNull(
                NavAction(
                    title = "id_add_note",
                    icon = "note_pencil",
                    isMenuEntry = false
                ) {
                    postEvent(LocalEvents.Note)
                },
                (NavAction(
                    title = "id_sign_transaction",
                    icon = "signature",
                    isMenuEntry = true
                ) {
                    postEvent(LocalEvents.SignTransaction(broadcastTransaction = false))
                }).takeIf { appInfo.isDevelopmentOrDebug },
            )
        )

        session.ifConnected {
            if (denomination != null && !denomination.isFiat) {
                _denomination.value = denomination
            }

            session.pendingTransaction?.also {
                viewModelScope.coroutineScope.launch {
                    if (appInfo.isDevelopmentOrDebug) {
                        logger.d { "Params: ${it.first}" }
                        logger.d { "Transaction: ${it.second}" }
                    }

                    _transactionConfirmLook.value = TransactionConfirmLook.create(
                        params = it.first,
                        transaction = it.second,
                        account = account,
                        session = session,
                        denomination = _denomination.value,
                        isAddressVerificationOnDevice = false
                    )

                    _isValid.value = true
                }

            } ?: run {
                postSideEffect(SideEffects.NavigateBack())
            }
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.SignTransaction -> {
                signTransaction(event.broadcastTransaction, event.twoFactorResolver)
            }

            is LocalEvents.SetNote -> {
                note.value = event.note
            }

            is LocalEvents.Note -> {
                postSideEffect(LocalSideEffects.Note(note.value))
            }
        }
    }

    private fun signTransaction(broadcast: Boolean, twoFactorResolver: TwoFactorResolver?) {
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
                    session.pendingTransaction = session.pendingTransaction?.let {
                        it.copy(first = params, second = tx)
                    }
                }
            }

            // If liquid, blind the transaction before signing
            if (account.network.isLiquid) {
                transaction = session.blindTransaction(account.network, transaction)
            }

            if (session.isHardwareWallet && !account.network.isLightning) {
                postSideEffect(
                    LocalSideEffects.DeviceAddressValidation(
                        TransactionConfirmLook.create(
                            params = params,
                            transaction = transaction,
                            account = account,
                            session = session,
                            denomination = _denomination.value,
                            isAddressVerificationOnDevice = true
                        )
                    )
                )
            }

            // Sign transaction
            val signedTransaction = session.signTransaction(account.network, transaction)

            // Broadcast or just sign
            if (broadcast) {
                if (signedTransaction.isSweep() || isSwap) {
                    session.broadcastTransaction(
                        network = account.network,
                        transaction = signedTransaction.transaction ?: ""
                    )
                } else {
                    session.sendTransaction(
                        account = account,
                        signedTransaction = signedTransaction,
                        twoFactorResolver = twoFactorResolver ?: this
                    )
                }
            } else {
                SendTransactionSuccess(signedTransaction = signedTransaction.transaction ?: "")
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            // Dismiss Verify Transaction Dialog
            postSideEffect(SideEffects.Dismiss)

            if (broadcast) {
                session.pendingTransaction?.third?.also {
                    countly.endSendTransaction(
                        session = session,
                        account = account,
                        transactionSegmentation = it,
                        withMemo = note.value.isNotBlank()
                    )
                }

                session.pendingTransaction = null // clear pending transaction
                postSideEffect(SideEffects.Snackbar("id_transaction_sent"))
                postSideEffect(SideEffects.NavigateToRoot)
            } else {
                onProgress.value = false
                postSideEffect(
                    SideEffects.Dialog(
                        title = "Signed Transaction",
                        message = it.signedTransaction ?: ""
                    )
                )
            }

        }, onError = {
            // Dismiss Verify Transaction Dialog
            postSideEffect(SideEffects.Dismiss)

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

            session.pendingTransaction?.also { pendingTransaction ->
                countly.failedTransaction(
                    session = session,
                    account = account,
                    transactionSegmentation = pendingTransaction.third,
                    error = it
                )
            }
        })
    }

    companion object: Loggable()
}

class SendConfirmViewModelPreview(
    greenWallet: GreenWallet,
    transactionConfirmLook: TransactionConfirmLook? = null
) :
    SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = previewAccountAsset()) {

    override val note: MutableStateFlow<String> = MutableStateFlow("")
    override val transactionConfirmLook: StateFlow<TransactionConfirmLook?> =
        MutableStateFlow(transactionConfirmLook)

    init {
        banner.value = Banner.preview3
        note.value = "Note"
    }

    companion object {
        fun preview() = SendConfirmViewModelPreview(
            previewWallet(), transactionConfirmLook = TransactionConfirmLook(
                from = previewAccountAsset(),
                utxos = listOf(
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        assetId = BTC_POLICY_ASSET,
                        amount = "2.123 BTC",
                        amountExchange = "45.123 USD"
                    )
                ),
                fee = "0.0123 BTC",
                feeFiat = "13,03 USD",
                feeRate = "1 vbyte/sats",
                total = "5.5 BTC",
                totalFiat = "143,234 USD"
            )
        )

        fun previewAccountExchange() = SendConfirmViewModelPreview(
            previewWallet(), transactionConfirmLook = TransactionConfirmLook(
                from = previewAccountAsset(),
                to = previewAccountAsset(),
                amount = "2.123 BTC",
                amountFiat = "43.312 USD",
                fee = "0.0123 BTC",
                feeFiat = "13,03 USD",
                feeRate = "1 vbyte/sats",
                total = "5.5 BTC",
                totalFiat = "143,234 USD"
            )
        )
    }
}
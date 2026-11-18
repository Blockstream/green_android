package com.blockstream.compose.models.send

import com.blockstream.compose.utils.StringHolder
import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_confirm_transaction
import blockstream_green.common.generated.resources.id_something_went_wrong
import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.toSwapFeesDestination
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.InvoiceType
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.lightning.invoiceType
import com.blockstream.data.lwk.invoiceType
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.send.mapLightningSendError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class SendLightningConfirmViewModelAbstract(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override fun screenName(): String = "SendLightningConfirm"

    abstract val invoice: String
    abstract val invoiceAmount: StateFlow<String?>
    abstract val invoiceAmountFiat: StateFlow<String?>
}

class SendLightningConfirmViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    override val invoice: String,
    private val amountSatoshi: Long?,
    denomination: Denomination?,
) : SendLightningConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val showVerifyOnDevice: StateFlow<Boolean> = MutableStateFlow(false)

    private val _transactionConfirmation: MutableStateFlow<TransactionConfirmation?> = MutableStateFlow(null)
    override val transactionConfirmation: StateFlow<TransactionConfirmation?> = _transactionConfirmation.asStateFlow()

    private val _invoiceAmount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val invoiceAmount: StateFlow<String?> = _invoiceAmount.asStateFlow()

    private val _invoiceAmountFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val invoiceAmountFiat: StateFlow<String?> = _invoiceAmountFiat.asStateFlow()

    private val _successAmount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val successAmount: StateFlow<String?> = _successAmount.asStateFlow()

    private val _failureMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    override val failureMessage: StateFlow<String?> = _failureMessage.asStateFlow()

    override val sentTxHash: StateFlow<String?> = MutableStateFlow(null)

    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_confirm_transaction),
            isCentered = true,
        )

        session.ifConnected {
            denomination?.takeIf { !it.isFiat }?.also { _denomination.value = it }

            session.pendingTransaction?.also { pending ->
                viewModelScope.launch {
                    tryCatch {
                        _transactionConfirmation.value = sendUseCase.getTransactionConfirmationUseCase(
                            params = pending.params,
                            transaction = pending.transaction,
                            account = account,
                            session = session,
                            denomination = _denomination.value,
                            isAddressVerificationOnDevice = false,
                        )

                        val addressee = pending.params.addresseesAsParams?.firstOrNull()
                        val txSatoshi = pending.transaction.satoshi.values.firstOrNull()?.let {
                            if (it < 0) -it else it
                        }
                        val invoiceSats = amountSatoshi?.takeIf { it > 0 }
                            ?: addressee?.satoshi?.takeIf { it > 0 }
                            ?: txSatoshi
                        val invoiceAssetId = addressee?.assetId
                            ?: pending.transaction.satoshi.keys.firstOrNull()

                        if (invoiceSats != null) {
                            _invoiceAmount.value = invoiceSats.toAmountLook(
                                session = session,
                                assetId = invoiceAssetId,
                                denomination = _denomination.value,
                                withUnit = true,
                                withGrouping = true,
                            )
                            _invoiceAmountFiat.value = invoiceSats.toAmountLook(
                                session = session,
                                assetId = invoiceAssetId,
                                denomination = Denomination.fiat(session),
                                withUnit = true,
                                withGrouping = true,
                            )
                        }

                        _isValid.value = true
                    } ?: run {
                        postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_something_went_wrong)))
                        postSideEffect(SideEffects.NavigateBack())
                    }
                }
            } ?: run {
                postSideEffect(SideEffects.NavigateBack())
            }
        }

        bootstrap()
    }

    override fun onSendSuccess(params: CreateTransactionParams?, txHash: String?) {
        _successAmount.value = _invoiceAmount.value
    }

    override fun onSendError(error: Throwable) {
        when (error.message) {
            "id_action_canceled" -> {
            }
            "id_transaction_already_confirmed" -> {
                super.onSendError(error)
            }
            else -> {
                _failureMessage.value = mapLightningSendError(error)
            }
        }
    }

    override fun onSuccessAcknowledged() {
        _successAmount.value = null
        postSideEffect(SideEffects.NavigateAfterSendTransaction)
    }

    override fun onFailureAcknowledged() {
        _failureMessage.value = null
    }

    class LocalEvents {
        object ClickTotalFees : Event
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is CreateTransactionViewModelAbstract.LocalEvents.SignTransaction) {
            session.pendingTransaction?.also { pending ->
                val invoiceType = resolveInvoiceType()
                countly.sendAttempt(session = session, account = account, invoiceType = invoiceType)
                sendLightningNativeTransaction(pending, invoiceType)
            }
        } else if (event is LocalEvents.ClickTotalFees) {
            _transactionConfirmation.value?.let { look ->
                postSideEffect(SideEffects.NavigateTo(look.toSwapFeesDestination()))
            }
        }
    }

    /**
     * Greenlight first: this is the greenlight send path, and lwk is absent on testnet, on wallets
     * without liquid, and on jade until swaps are enabled. Lwk stays as the fallback because the
     * greenlight parser has no BOLT12 variant.
     */
    private suspend fun resolveInvoiceType(): InvoiceType =
        tryCatch { session.lightningSdkOrNull?.parseBoltOrLNUrlAndCache(invoice)?.invoiceType() }
            ?: tryCatch { session.lwkOrNull?.inspectPaymentInstruction(invoice)?.invoiceType() }
            ?: InvoiceType.UNKNOWN

    private fun sendLightningNativeTransaction(pending: PendingTransaction, invoiceType: InvoiceType) {
        doAsync({
            countly.startSendTransaction()
            countly.startFailedTransaction()

            val tx = session.createTransaction(account = account, params = pending.params)
            session.sendLightningTransaction(params = tx, comment = note.value)
        }, preAction = {
            _onProgressSending.value = true
            onProgress.value = true
        }, postAction = {
            val isSuccess = it == null
            _onProgressSending.value = isSuccess
            onProgress.value = isSuccess
        }, onSuccess = {
            session.pendingTransaction = null
            countly.endSendTransaction(
                session = session,
                account = account,
                transactionSegmentation = pending.segmentation,
                withMemo = note.value.isNotBlank(),
                invoiceType = invoiceType
            )
            onSendSuccess(pending.params)
        }, onError = {
            onSendError(it)
            countly.failedTransaction(
                session = session,
                account = account,
                transactionSegmentation = pending.segmentation,
                error = it,
                invoiceType = invoiceType
            )
        })
    }
}

class SendLightningConfirmViewModelPreview(greenWallet: GreenWallet) :
    SendLightningConfirmViewModelAbstract(
        greenWallet = greenWallet,
        accountAsset = previewAccountAsset(isLightning = true),
    ) {
    override val invoice: String =
        "lnbc10u1pj7w0nml0f39p5p2459s8178ahsanrjcg6vyzqkmasvjcg7ycz3jhg9wmgxrwzg7y6f7udj9c5zh"
    override val showVerifyOnDevice: StateFlow<Boolean> = MutableStateFlow(false)
    override val transactionConfirmation: StateFlow<TransactionConfirmation?> = MutableStateFlow(null)
    override val invoiceAmount: StateFlow<String?> = MutableStateFlow("0.00005265 BTC")
    override val invoiceAmountFiat: StateFlow<String?> = MutableStateFlow("300.00 USD")
    override val successAmount: StateFlow<String?> = MutableStateFlow(null)
    override val failureMessage: StateFlow<String?> = MutableStateFlow(null)
    override val sentTxHash: StateFlow<String?> = MutableStateFlow(null)

    override fun onSuccessAcknowledged() {}
    override fun onFailureAcknowledged() {}

    companion object {
        fun preview() = SendLightningConfirmViewModelPreview(previewWallet())
    }
}

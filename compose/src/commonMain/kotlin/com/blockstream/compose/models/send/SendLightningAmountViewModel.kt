package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_bolt12_amount
import blockstream_green.common.generated.resources.id_lnurl_amount
import blockstream_green.common.generated.resources.id_send_lightning_bitcoin
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.getNetworkIcon
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.lightning.maxPayableSatoshi
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapAsset
import com.blockstream.data.swap.SwapDetails
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.toAmountLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class SendLightningAmountViewModelAbstract(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    CreateTransactionViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "SendLightningAmount"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    abstract val address: String
    abstract val amount: MutableStateFlow<String>
    abstract val amountExchange: StateFlow<String>
}

class SendLightningAmountViewModel(
    greenWallet: GreenWallet,
    override val address: String,
    addressType: AddressInputType,
    accountAsset: AccountAsset,
) : SendLightningAmountViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    private val _instruction: MutableStateFlow<PaymentInstruction?> = MutableStateFlow(null)
    private val _quote: MutableStateFlow<Quote?> = MutableStateFlow(null)

    override val amountExchange: StateFlow<String> = amount
        .map { input ->
            session.ifConnected {
                val assetId = accountAsset.assetId
                UserInput.parseUserInputSafe(
                    session = session,
                    input = input,
                    assetId = assetId,
                    denomination = denomination.value,
                ).getBalance()?.let { balance ->
                    val exchangeDenom = Denomination.exchange(session, denomination.value)
                    balance.toAmountLook(
                        session = session,
                        assetId = assetId,
                        denomination = exchangeDenom,
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = false,
                    )?.let { formatted -> "≈ $formatted" }
                }
            } ?: ""
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    init {
        _addressInputType = addressType
        _network.value = accountAsset.account.network

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_send_lightning_bitcoin),
                titleIcon = accountAsset.account.network.id.getNetworkIcon(),
                isCentered = true,
            )
        }

        viewModelScope.launch {
            _instruction.value = tryCatch {
                session.lwkOrNull?.inspectPaymentInstruction(address)
            }
            val titleRes = when (_instruction.value) {
                is PaymentInstruction.Bolt12 -> Res.string.id_bolt12_amount
                is PaymentInstruction.LnUrl -> Res.string.id_lnurl_amount
                else -> Res.string.id_send_lightning_bitcoin
            }
            _navData.value = _navData.value.copy(title = getString(titleRes))
        }

        @OptIn(FlowPreview::class)
        amount
            .debounce(QUOTE_REFRESH_DEBOUNCE_MS)
            .map { input ->
                val sats = UserInput.parseUserInputSafe(
                    session = session,
                    input = input,
                    denomination = denomination.value,
                ).getBalance()?.satoshi ?: 0L
                sats.coerceAtLeast(QUOTE_SAMPLE_SATS)
            }
            .distinctUntilChanged()
            .mapLatest { sats ->
                tryCatch {
                    session.lwkOrNull?.quote(
                        satoshi = sats,
                        quoteMode = QuoteMode.SEND,
                        send = SwapAsset.Liquid,
                        receive = SwapAsset.Lightning,
                    )
                }
            }
            .onEach { _quote.value = it }
            .launchIn(this)

        @OptIn(FlowPreview::class)
        combine(amount, _instruction, _quote, accountAssetBalance) { _, _, _, _ -> Unit }
            .debounce(PARAMS_REFRESH_DEBOUNCE_MS)
            .mapLatest {
                tryCatch(context = Dispatchers.Default) { softValidate() }
            }
            .onEach { applyValidation(it) }
            .launchIn(this)

        bootstrap()
    }

    override suspend fun createTransactionParams(): CreateTransactionParams? {
        val account = accountAsset.value ?: return null
        if (amount.value.isBlank()) return null
        return sendUseCase.prepareTransactionUseCase(
            greenWallet = greenWallet,
            session = session,
            accountAsset = account,
            address = address,
            amount = amount.value,
            denomination = denomination.value,
            paymentInstruction = _instruction.value,
        )
    }

    override fun createTransaction(
        params: CreateTransactionParams?,
        finalCheckBeforeContinue: Boolean,
    ) {
        if (params == null) {
            _isValid.value = false
            _error.value = null
            return
        }
        val account = accountAsset.value ?: return
        doAsync({
            val tx = session.createTransaction(account.account.network, params)
            if (!tx.error.isNullOrBlank()) {
                throw Exception(tx.error)
            }
            tx
        }, mutex = createTransactionMutex, onSuccess = { tx ->
            if (tx != null) {
                _isValid.value = true
                _error.value = null

                if (finalCheckBeforeContinue) {
                    val amountSats = UserInput.parseUserInputSafe(
                        session = session,
                        input = amount.value,
                        denomination = denomination.value,
                    ).getBalance()?.satoshi
                    session.pendingTransaction = PendingTransaction(
                        params = params,
                        transaction = tx,
                        segmentation = TransactionSegmentation(
                            transactionType = TransactionType.SEND,
                            addressInputType = _addressInputType,
                            sendAll = false,
                        ),
                    )
                    val destination = if (account.account.network.isLightning) {
                        NavigateDestinations.SendLightningConfirm(
                            greenWallet = greenWallet,
                            accountAsset = account,
                            invoice = address,
                            amountSatoshi = amountSats,
                            denomination = denomination.value,
                        )
                    } else {
                        NavigateDestinations.SendConfirm(
                            greenWallet = greenWallet,
                            accountAsset = account,
                            denomination = denomination.value,
                        )
                    }
                    postSideEffect(SideEffects.NavigateTo(destination))
                }
            }
        }, onError = { err ->
            _isValid.value = false
            _error.value = mapSendError(err.message, amount.value, params.swap)
        })
    }

    override suspend fun denominatedValue(): DenominatedValue? {
        val assetId = accountAsset.value?.assetId ?: return null
        return UserInput.parseUserInputSafe(
            session = session,
            input = amount.value,
            denomination = denomination.value,
            assetId = assetId,
        ).getBalance().let {
            DenominatedValue(
                balance = it,
                assetId = assetId,
                denomination = denomination.value,
            )
        }
    }

    override fun setDenominatedValue(denominatedValue: DenominatedValue) {
        _denomination.value = denominatedValue.denomination
        amount.value = denominatedValue.asInput ?: ""
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            doAsync({
                createTransactionParams() ?: throw Exception("id_action_canceled")
            }, onSuccess = { params ->
                if (params != null) {
                    createTransaction(params = params, finalCheckBeforeContinue = true)
                }
            }, onError = { err ->
                _isValid.value = false
                _error.value = mapSendError(err.message, amount.value, null)
            })
        }
    }

    private sealed interface SoftValidation {
        object Idle : SoftValidation
        object Valid : SoftValidation
        data class Invalid(val error: String?) : SoftValidation
    }

    private suspend fun softValidate(): SoftValidation {
        if (amount.value.isBlank()) return SoftValidation.Idle
        val account = accountAsset.value ?: return SoftValidation.Idle
        val sats = UserInput.parseUserInputSafe(
            session = session,
            input = amount.value,
            denomination = denomination.value,
        ).getBalance()?.satoshi ?: 0L
        if (sats <= 0L) return SoftValidation.Idle

        if (account.account.network.isLightning) {
            val maxPayable = session.lightningSdkOrNull?.nodeInfoStateFlow?.value?.maxPayableSatoshi() ?: 0L
            if (maxPayable == 0L) {
                return SoftValidation.Invalid("id_lightning_balance_too_low_to_send")
            }
            if (sats > maxPayable) {
                return SoftValidation.Invalid("id_amount_is_above_the_maximum_payment_limit_of_s|${formatSats(maxPayable)}")
            }
            return SoftValidation.Valid
        }

        val balance = account.balance(session)
        val quote = _quote.value
        if (quote != null) {
            if (sats > quote.maximal) {
                return SoftValidation.Invalid("id_amount_is_above_the_maximum_payment_limit_of_s|${formatSats(quote.maximal)}")
            }
            val required = sats + quote.boltzFee + quote.claimNetworkFee
            if (required > balance) {
                val deficit = required - balance
                return SoftValidation.Invalid("id_not_enough_lbtc_to_cover_swap_fees_s|${formatSats(deficit)}")
            }
        } else if (sats > balance) {
            return SoftValidation.Invalid("id_insufficient_funds")
        }

        return SoftValidation.Valid
    }

    private fun applyValidation(result: SoftValidation?) {
        when (result) {
            is SoftValidation.Valid -> {
                _isValid.value = true
                _error.value = null
            }
            is SoftValidation.Invalid -> {
                _isValid.value = false
                _error.value = result.error
            }
            null, SoftValidation.Idle -> {
                _isValid.value = false
                _error.value = null
            }
        }
    }

    private suspend fun mapSendError(
        message: String?,
        input: String,
        swap: SwapDetails?,
    ): String? {
        if (message != "id_insufficient_funds") return message
        val sats = UserInput.parseUserInputSafe(
            session = session,
            input = input,
            denomination = denomination.value,
        ).getBalance()?.satoshi ?: return message

        val account = accountAsset.value?.account
        if (account?.network?.isLightning == true) {
            val maxPayable = session.lightningSdkOrNull?.nodeInfoStateFlow?.value?.maxPayableSatoshi() ?: 0L
            if (maxPayable == 0L) {
                return "id_lightning_balance_too_low_to_send"
            }
            if (sats > maxPayable) {
                return "id_amount_is_above_the_maximum_payment_limit_of_s|${formatSats(maxPayable)}"
            }
        }

        if (swap != null && account != null) {
            val deficit = swap.fromAmount - account.balance(session)
            if (deficit > 0L) {
                return "id_not_enough_lbtc_to_cover_swap_fees_s|${formatSats(deficit)}"
            }
        }

        return "id_insufficient_funds_invoice_amount_s|${formatSats(sats)}"
    }

    private suspend fun formatSats(satoshi: Long): String {
        val sats = satoshi.toAmountLook(
            session = session,
            denomination = Denomination.SATOSHI,
            withUnit = true,
            withGrouping = true,
        ) ?: "$satoshi"
        val fiat = Denomination.fiat(session)?.let {
            satoshi.toAmountLook(
                session = session,
                denomination = it,
                withUnit = true,
                withGrouping = true,
            )
        }
        return if (fiat != null) "$sats ($fiat)" else sats
    }

    companion object {
        private const val QUOTE_SAMPLE_SATS = 1_000L
        private const val QUOTE_REFRESH_DEBOUNCE_MS = 400L
        private const val PARAMS_REFRESH_DEBOUNCE_MS = 400L
    }
}

class SendLightningAmountViewModelPreview(greenWallet: GreenWallet) :
    SendLightningAmountViewModelAbstract(
        greenWallet = greenWallet,
        accountAsset = previewAccountAsset(isLightning = true),
    ) {

    override val address: String = "lnbc..."
    override val amount: MutableStateFlow<String> = MutableStateFlow("")
    override val amountExchange: StateFlow<String> = MutableStateFlow("≈ 0,00 USD")

    companion object {
        fun preview() = SendLightningAmountViewModelPreview(previewWallet())
    }
}

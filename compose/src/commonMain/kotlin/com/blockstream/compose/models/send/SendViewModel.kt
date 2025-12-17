package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_limits_s__s
import blockstream_green.common.generated.resources.id_payment_requested_by_s
import blockstream_green.common.generated.resources.id_send
import blockstream_green.common.generated.resources.id_transaction_sent
import blockstream_green.common.generated.resources.note_pencil
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.sheets.NoteType
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.compose.utils.getStringFromId
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.banner.Banner
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.ExceptionWithSupportData
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SupportData
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.isBlank
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.extensions.startsWith
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.lightning.lnUrlPayDescription
import com.blockstream.data.lightning.lnUrlPayImage
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.ifNotNull
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.swap.SwapUseCase
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import kotlin.io.encoding.Base64
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.minutes

abstract class SendViewModelAbstract(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    CreateTransactionViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "Send"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    abstract val address: String

    abstract val errorAmount: StateFlow<String?>

    abstract val errorGeneric: StateFlow<String?>

    abstract val amount: MutableStateFlow<String>

    abstract val amountExchange: StateFlow<String>

    abstract val amountHint: StateFlow<String?>

    abstract val showAmount: StateFlow<Boolean>

    abstract val isAmountLocked: StateFlow<Boolean>

    abstract val isSendAll: MutableStateFlow<Boolean>

    abstract val supportsSendAll: Boolean

    abstract val description: StateFlow<String?>

    abstract val metadataDomain: StateFlow<String?>

    abstract val metadataImage: StateFlow<ByteArray?>

    abstract val metadataDescription: StateFlow<String?>

    abstract val isNoteEditable: StateFlow<Boolean>
}

class SendViewModel(
    greenWallet: GreenWallet,
    override val address: String,
    addressType: AddressInputType,
    accountAsset: AccountAsset
) : SendViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    internal val boltzUseCase: SwapUseCase by inject()

    override val supportsSendAll: Boolean = !accountAsset.account.isLightning

    override val isSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _errorAmount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorAmount: StateFlow<String?> = _errorAmount.asStateFlow()

    private val _errorGeneric: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorGeneric: StateFlow<String?> = _errorGeneric.asStateFlow()

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    override val amountExchange: StateFlow<String> = amount.map { amount ->
        session.ifConnected {
            accountAsset.assetId.takeIf { it.isPolicyAsset(session) }?.let { assetId ->
                UserInput.parseUserInputSafe(
                    session = session,
                    input = amount,
                    assetId = assetId,
                    denomination = denomination.value
                ).getBalance()?.let {
                    "≈ " + it.toAmountLook(
                        session = session,
                        assetId = assetId,
                        denomination = Denomination.exchange(session, denomination.value),
                        withUnit = true,
                        withGrouping = true,
                        withMinimumDigits = false
                    )
                }
            }
        } ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val _amountHint: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amountHint: StateFlow<String?> = _amountHint.asStateFlow()

    private val _isAmountLocked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isAmountLocked: StateFlow<Boolean> = _isAmountLocked.asStateFlow()

    private val _showAmount: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showAmount: StateFlow<Boolean> = _showAmount.asStateFlow()

    private val _description: MutableStateFlow<String?> = MutableStateFlow(null)
    override val description: StateFlow<String?> = _description

    private val _metadataDomain: MutableStateFlow<String?> = MutableStateFlow(null)
    override val metadataDomain: StateFlow<String?> = _metadataDomain.asStateFlow()

    private val _metadataImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val metadataImage: StateFlow<ByteArray?> = _metadataImage.asStateFlow()

    private val _metadataDescription: MutableStateFlow<String?> = MutableStateFlow(null)
    override val metadataDescription: StateFlow<String?> = _metadataDescription.asStateFlow()

    private val _isNoteEditable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isNoteEditable: StateFlow<Boolean> = _isNoteEditable

    class LocalEvents {
        object ToggleIsSendAll : Event
        object SendLightningTransaction : Event
        object Note : Event
    }

    private var addressNetwork: MutableStateFlow<Network?> = MutableStateFlow(null)

    init {
        _addressInputType = addressType
        _network.value = accountAsset.account.network

        isNoteEditable.onEach { isEditable ->
            _navData.value = NavData(
                title = getString(Res.string.id_send),
                actions = listOfNotNull(
                    (NavAction(
                        title = getString(Res.string.id_add_note),
                        icon = Res.drawable.note_pencil,
                        isMenuEntry = false
                    ) {
                        postEvent(LocalEvents.Note)
                    }).takeIf { isEditable }
                ))
        }.launchIn(this)

        combine(isNoteEditable, onProgressSending) { isNoteEditable, onProgressSending ->
            _navData.value = NavData(
                title = getString(Res.string.id_send),
                actions = listOfNotNull(
                    (NavAction(
                        title = getString(Res.string.id_add_note),
                        icon = Res.drawable.note_pencil,
                        isMenuEntry = false
                    ) {
                        postEvent(LocalEvents.Note)
                    }).takeIf { isNoteEditable }
                ),
                isVisible = !onProgressSending
            )
        }.launchIn(this)

        session.ifConnected {

            var isSwapsEnabled = false

            viewModelScope.launch {
                isSwapsEnabled = boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet)
                addressNetwork.value = tryCatch { session.parseInput(address)?.first }
            }

            combine(
                addressNetwork.filterNotNull(),
                _feeEstimation,
                amount,
                isSendAll,
                _feePriorityPrimitive,
                merge(
                    flowOf(Unit),
                    session.accountsAndBalanceUpdated
                ), // set initial value, watch for wallet balance updates, especially on wallet startup like bip39 uris
            ) {
                // Prefer account network as this can be a swap
                val accountNetwork = accountAsset.account.network

                _showFeeSelector.value = sendUseCase.showFeeSelectorUseCase(session = session, network = accountNetwork)

                _showAmount.value = !isSwapsEnabled || !(addressNetwork.value?.isLightning == true && accountAsset.account.network.isLiquid)

                createTransactionParams.value = tryCatch(context = Dispatchers.Default) { createTransactionParams() }
            }.launchIn(this)

            error.onEach {
                _errorAmount.value = it.takeIf {
                    listOf(
                        "id_invalid_amount",
                        "id_insufficient_funds",
                        "id_amount_must_be_at_least_s",
                        "id_amount_must_be_at_most_s",
                        "id_amount_below_the_dust_threshold",
                        "id_amount_above_maximum_allowed",
                        "id_amount_below_minimum_allowed"
                    ).startsWith(it)
                }?.let { getStringFromId(it) }
                _errorGeneric.value = it.takeIf { _errorAmount.value.isNullOrBlank() }?.let {
                    getStringFromId(it)
                }
            }.launchIn(this)
        }

        // In case onProgress goes to false from another doAsync while onProgressSending is still true
        onProgress.onEach {
            if (!it && _onProgressSending.value) {
                onProgress.value = _onProgressSending.value
            }
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {

            is LocalEvents.ToggleIsSendAll -> {
                isSendAll.value = isSendAll.value.let { isSendAll ->
                    if (isSendAll) {
                        amount.value = ""
                    }
                    !isSendAll
                }
            }

            is Events.Continue -> {
                createTransactionParams.value?.also {
                    createTransaction(params = it, finalCheckBeforeContinue = true)
                }
            }

            is LocalEvents.SendLightningTransaction -> {
                sendLightningTransaction()
            }

            is LocalEvents.Note -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Note(
                            greenWallet = greenWallet,
                            note = note.value,
                            noteType = if (isNoteEditable.value) NoteType.Comment else NoteType.Description // LNURL allows to add a comment, Bolt11 includes a description
                        )
                    )
                )
            }
        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams {
        return sendUseCase.prepareTransactionUseCase(
            greenWallet = greenWallet,
            session = session,
            accountAsset = accountAsset.value!!,
            address = address,
            amount = amount.value,
            denomination = denomination.value,
            isSendAll = isSendAll.value,
            feeRate = getFeeRate()
        )
    }

    override fun createTransaction(
        params: CreateTransactionParams?,
        finalCheckBeforeContinue: Boolean
    ) {
        doAsync({
            if (params == null) {
                _isAmountLocked.value = false
                _metadataDomain.value = null
                _metadataImage.value = null
                _metadataDescription.value = null
                note.value = ""
                _isNoteEditable.value = false
                return@doAsync null
            }

            val isSwap = addressNetwork.value?.isLightning == true && accountAsset.value?.account?.network?.isLiquid == true

            accountAsset.value?.let { accountAsset ->

                val network = accountAsset.account.network

                val tx = session.createTransaction(network, params)

                _isNoteEditable.value = tx.isLightningDescriptionEditable

                // Clear error as soon as possible
                if (tx.error.isBlank()) {
                    _error.value = null
                }

                // Mainly used in Lightning invoice
                _description.value = tx.memo

                tx.addressees.firstOrNull()?.also { addressee ->
                    _isAmountLocked.value = addressee.isAmountLocked == true || isSwap

                    _metadataDomain.value = addressee.domain?.let { getString(Res.string.id_payment_requested_by_s, it) }
                    _metadataImage.value = addressee.metadata.lnUrlPayImage()
                    _metadataDescription.value = addressee.metadata.lnUrlPayDescription()

                    _amountHint.value = if (!addressee.isAmountLocked) {
                        ifNotNull(
                            addressee.minAmount,
                            addressee.maxAmount
                        ) { minAmount, maxAmount ->
                            getString(
                                Res.string.id_limits_s__s, minAmount.toAmountLook(
                                    session = session,
                                    withUnit = false
                                ) ?: "", maxAmount.toAmountLook(
                                    session = session,
                                    withUnit = true
                                ) ?: ""
                            )
                        }
                    } else null

                    if (addressee.bip21Params?.hasAmount == true || addressee.isGreedy == true || addressee.isAmountLocked == true || isSwap) {
                        val assetId = addressee.assetId ?: account.network.policyAsset

                        if (!assetId.isPolicyAsset(account.network) && denomination.value.isFiat) {
                            _denomination.value = Denomination.default(session)
                        }

                        (tx.satoshi[assetId]?.absoluteValue?.let { sendAmount ->
                            sendAmount.toAmountLook(
                                session = session,
                                assetId = assetId,
                                denomination = denomination.value,
                                withUnit = false,
                                withGrouping = false
                            )
                        } ?: tx.addressees.firstOrNull()?.bip21Params?.amount?.let { bip21Amount ->
                            session.convert(
                                assetId = assetId,
                                asString = bip21Amount
                            )?.toAmountLook(
                                session = session,
                                assetId = assetId,
                                withUnit = false,
                                withGrouping = false,
                                withMinimumDigits = false,
                                denomination = denomination.value,
                            )
                        }).also {
                            amount.value = it ?: ""
                        }
                    }

                }

                tx.fee?.takeIf { it != 0L || tx.error.isNullOrBlank() }.also {
                    _feePriority.value = calculateFeePriority(
                        session = session,
                        feePriority = _feePriority.value,
                        feeAmount = it,
                        feeRate = tx.feeRate?.feeRateWithUnit()
                    )
                }

                tx.error.takeIf { it.isNotBlank() }?.also {
                    // If amount is blank and not SendAll, skip displaying an error, else user will immediately see the error when starts typing
                    if ((amount.value.isBlank() && !isSendAll.value) && listOf(
                            "id_invalid_amount",
                            "id_amount_below_the_dust_threshold",
                            "id_insufficient_funds",
                            "id_amount_must_be_at_least_s",
                            "id_amount_must_be_at_most_s"
                        ).startsWith(it)
                    ) {
                        // clear error
                        _error.value = null
                        return@doAsync null
                    }

                    if (it == "id_amount_below_the_dust_threshold" && params.addresseesAsParams?.firstOrNull()
                            ?.let { it.assetId.isPolicyAsset(session) && !it.isGreedy && it.satoshi < DustLimit } == true
                    ) {
                        throw Exception("id_amount_must_be_at_least_s|$DustLimit sats")
                    } else {
                        throw Exception(it)
                    }
                }

                tx
            }

        }, mutex = createTransactionMutex, onSuccess = {
            _isValid.value = it != null
            if (it != null) {
                // Preserve error
                _error.value = null
            }

            if (finalCheckBeforeContinue && params != null && it != null) {
                session.pendingTransaction = PendingTransaction(
                    params = params,
                    transaction = it,
                    segmentation = TransactionSegmentation(
                        transactionType = TransactionType.SEND,
                        addressInputType = _addressInputType,
                        sendAll = isSendAll.value
                    )
                )
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendConfirm(
                            greenWallet = greenWallet,
                            accountAsset = accountAsset.value!!,
                            denomination = denomination.value
                        )
                    )
                )
            }
        }, onError = {
            _isValid.value = false
            _error.value = it.message
        })
    }

    override suspend fun denominatedValue(): DenominatedValue? {
        return accountAsset.value?.let { accountAsset ->
            UserInput.parseUserInputSafe(
                session = session,
                input = amount.value,
                denomination = denomination.value,
                assetId = accountAsset.assetId
            ).getBalance().let {
                DenominatedValue(
                    balance = it,
                    assetId = accountAsset.assetId,
                    denomination = denomination.value
                )
            }
        }

    }

    private fun sendLightningTransaction() {
        doAsync({
            countly.startSendTransaction()
            countly.startFailedTransaction()
            
            GlobalScope.async(context = Dispatchers.IO) {
                withTimeout(1.minutes) {
                    createTransactionParams.value?.let {
                        session.sendLightningTransaction(params = session.createTransaction(_network.value!!, it), comment = note.value)
                    } ?: run {
                        throw Exception("Something went wrong while creating the Transaction")
                    }
                }
            }.await()

        }, timeout = 1.minutes, preAction = {
            _onProgressSending.value = true
            onProgress.value = true
        }, postAction = {
            val isSuccess = it == null
            _onProgressSending.value = isSuccess
            onProgress.value = isSuccess
        }, onSuccess = {
            countly.endSendTransaction(
                session = session,
                account = account,
                transactionSegmentation = TransactionSegmentation(
                    transactionType = TransactionType.SEND,
                    addressInputType = _addressInputType
                ),
                withMemo = false
            )

            if (it.hasMessageOrUrl) {
                postSideEffect(SideEffects.TransactionSent(it))
            } else {
                postSideEffect(SideEffects.NavigateAfterSendTransaction)
            }

            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_transaction_sent)))

        }, onError = {

            postSideEffect(
                SideEffects.ErrorDialog(
                    error = it, supportData = (it as? ExceptionWithSupportData)?.supportData
                        ?: SupportData.create(
                            throwable = it,
                            network = account.network,
                            session = session
                        )
                )
            )

            countly.failedTransaction(
                session = session,
                account = account,
                transactionSegmentation = TransactionSegmentation(
                    transactionType = TransactionType.SEND,
                    addressInputType = _addressInputType
                ),
                error = it
            )
        })
    }

    override fun setDenominatedValue(denominatedValue: DenominatedValue) {
        _denomination.value = denominatedValue.denomination
        amount.value = denominatedValue.asInput ?: ""
    }

    companion object : Loggable() {
        val DustLimit = 546
    }
}

class SendViewModelPreview(greenWallet: GreenWallet, isLightning: Boolean = false) :
    SendViewModelAbstract(greenWallet = greenWallet, accountAsset = previewAccountAsset(isLightning = isLightning)) {
    override val errorAmount: StateFlow<String?> = MutableStateFlow(null)
    override val errorGeneric: StateFlow<String?> = MutableStateFlow(null)

    private val base64Png =
        "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"

    override val address: String = "address"
    override val amount: MutableStateFlow<String> = MutableStateFlow("0.1")
    override val amountExchange: StateFlow<String> = MutableStateFlow("0.1 USD")
    override val amountHint: StateFlow<String?> = MutableStateFlow(null)
    override val showAmount: StateFlow<Boolean> = MutableStateFlow(true)
    override val isAmountLocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val isSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val supportsSendAll: Boolean = true
    override val metadataDomain: StateFlow<String?> = MutableStateFlow("id_payment_requested_by_s|blockstream.com")
    override val metadataImage: StateFlow<ByteArray?> = MutableStateFlow(Base64.decode(base64Png))
    override val metadataDescription: StateFlow<String?> = MutableStateFlow("Metadata Description")
    override val description: StateFlow<String?> = MutableStateFlow(null)
    override val isNoteEditable: StateFlow<Boolean> = MutableStateFlow(true)

    init {
        _showFeeSelector.value = true
        banner.value = Banner.preview3
    }

    companion object {
        fun preview(isLightning: Boolean = false) = SendViewModelPreview(previewWallet(), isLightning = isLightning)
    }
}

package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_increase_fee
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

abstract class BumpViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) :
    CreateTransactionViewModelAbstract(
        greenWallet = greenWallet,
        accountAssetOrNull = accountAsset
    ) {
    override fun screenName(): String = "Bump"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    @NativeCoroutinesState
    abstract val address: StateFlow<String?>

    @NativeCoroutinesState
    abstract val amount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val amountFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val total: StateFlow<String?>

    @NativeCoroutinesState
    abstract val totalFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val oldFee: StateFlow<String?>

    @NativeCoroutinesState
    abstract val oldFeeFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val oldFeeRate: StateFlow<String?>
}

class BumpViewModel(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    transactionAsString: String
) :
    BumpViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    private val _address: MutableStateFlow<String?> = MutableStateFlow(null)
    override val address: StateFlow<String?> = _address.asStateFlow()

    private val _amount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amount: StateFlow<String?> = _amount.asStateFlow()

    private val _amountFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amountFiat: StateFlow<String?> = _amountFiat.asStateFlow()

    private val _total: MutableStateFlow<String?> = MutableStateFlow(null)
    override val total: StateFlow<String?> = _total.asStateFlow()

    private val _totalFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val totalFiat: StateFlow<String?> = _totalFiat.asStateFlow()

    private val _oldFee: MutableStateFlow<String?> = MutableStateFlow(null)
    override val oldFee: StateFlow<String?> = _oldFee.asStateFlow()

    private val _oldFeeFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val oldFeeFiat: StateFlow<String?> = _oldFeeFiat.asStateFlow()

    private val _oldFeeRate: MutableStateFlow<String?> = MutableStateFlow(null)
    override val oldFeeRate: StateFlow<String?> = _oldFeeRate.asStateFlow()

    private val transaction = Json.parseToJsonElement(transactionAsString)

    init {

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_increase_fee),
                subtitle = account.name
            )
        }

        // Always show fee selector
        _showFeeSelector.value = true

        session.ifConnected {
            _network.value = account.network

            combine(_feePriorityPrimitive, _feeEstimation.filterNotNull()) { _, _ ->
                createTransactionParams.value = createTransactionParams()
            }.launchIn(this)
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SignTransaction) {
            signAndSendTransaction(
                params = createTransactionParams.value,
                originalTransaction = createTransaction.value,
                segmentation = TransactionSegmentation(
                    transactionType = TransactionType.BUMP
                ),
                broadcast = event.broadcastTransaction
            )
        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams? {
        return try {
            val unspentOutputs = session.getUnspentOutputs(account = account, isBump = true)

            CreateTransactionParams(
                subaccount = account.pointer,
                feeRate = getFeeRate(),
                utxos = unspentOutputs.unspentOutputsAsJsonElement,
                previousTransaction = transaction
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _error.value = e.message
            null
        }
    }

    override fun createTransaction(
        params: CreateTransactionParams?,
        finalCheckBeforeContinue: Boolean
    ) {
        doAsync({
            if (params == null) {
                return@doAsync null
            }

            val tx = session.createTransaction(account.network, params)

            _address.value = tx.addressees.firstOrNull()?.address

            // Display amount info only for simple outgoing transactions
            if (tx.previousTransaction?.txType == Transaction.Type.OUT) {
                (if (tx.addressees.firstOrNull()?.isGreedy == true) {
                    tx.satoshi[account.network.policyAsset]
                } else {
                    tx.addressees.firstOrNull()?.satoshi
                }).also {
                    _amount.value = it.toAmountLook(
                        session = session,
                        withUnit = true,
                        withDirection = true
                    )

                    _amountFiat.value = it.toAmountLook(
                        session = session,
                        withUnit = true,
                        withDirection = true,
                        denomination = Denomination.fiat(session)
                    )
                }
            }

            _oldFee.value = tx.oldFee?.toAmountLook(session = session, withUnit = true)
            _oldFeeFiat.value = tx.oldFee?.toAmountLook(
                session = session,
                withUnit = true,
                denomination = Denomination.fiat(session)
            )
            _oldFeeRate.value = tx.oldFeeRate?.feeRateWithUnit()

            val feeErrors =
                listOf("id_invalid_replacement_fee_rate", "id_fee_rate_is_below_minimum")
            tx.fee?.takeIf { it != 0L || tx.error.isNullOrBlank() }.also {
                _feePriority.value = calculateFeePriority(
                    session = session,
                    feePriority = _feePriority.value,
                    feeAmount = it,
                    feeRate = tx.feeRate?.feeRateWithUnit(),
                    error = tx.error.takeIf { feeErrors.contains(it) }
                )
            }

            // Change fee
            if ((tx.feeRate ?: 0) < (tx.oldFeeRate ?: 0)) {
                _customFeeRate.value =
                    ((tx.oldFeeRate ?: account.network.defaultFee) / 1000.0 + minFee()).also {
                        _feePriority.value = FeePriority.Custom(it)
                    }
            }

            tx.error.takeIf { it.isNotBlank() }?.also {
                throw Exception(it)
            }

            tx

        }, mutex = createTransactionMutex, preAction = {
            _isValid.value = false
        }, postAction = {

        }, onSuccess = {
            createTransaction.value = it
            _isValid.value = it != null
            _error.value = null
        }, onError = {
            createTransaction.value = null
            _isValid.value = false
            _error.value = it.message
        })
    }

    companion object : Loggable()
}

class BumpViewModelPreview(greenWallet: GreenWallet) :
    BumpViewModelAbstract(greenWallet = greenWallet, accountAsset = previewAccountAsset()) {

    override val address: StateFlow<String?> =
        MutableStateFlow("bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
    override val amount: StateFlow<String?> = MutableStateFlow("1.0 BTC")
    override val amountFiat: StateFlow<String?> = MutableStateFlow("~ 150.000 USD")
    override val total: StateFlow<String?> = MutableStateFlow("150 BTC")
    override val totalFiat: StateFlow<String?> = MutableStateFlow("~ 150.000 USD")
    override val oldFee: StateFlow<String?> = MutableStateFlow("1,000 L-BTC")
    override val oldFeeFiat: StateFlow<String?> = MutableStateFlow("~ 5.00 USD")
    override val oldFeeRate: StateFlow<String?> = MutableStateFlow("1 sats/vbyte")

    init {
        _feePriority.value = FeePriority.Low(
            fee = "0.000001 BTC",
            feeFiat = "13.00 USD",
            feeRate = 2L.feeRateWithUnit(),
            expectedConfirmationTime = "id_s_hours|2"
        )
    }

    companion object {
        fun preview() = BumpViewModelPreview(previewWallet())
    }
}
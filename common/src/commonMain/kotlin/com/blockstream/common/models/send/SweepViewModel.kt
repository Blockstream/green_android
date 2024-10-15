package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_sweep
import com.blockstream.common.AddressInputType
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.data.Redact
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.gdk.params.toJsonElement
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class SweepViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null
) :
    CreateTransactionViewModelAbstract(
        greenWallet = greenWallet,
        accountAssetOrNull = accountAssetOrNull
    ) {
    override fun screenName(): String = "Sweep"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    @NativeCoroutinesState
    abstract val accounts: StateFlow<List<AccountAssetBalance>>

    @NativeCoroutinesState
    abstract val privateKey: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val amountFiat: StateFlow<String?>
}

class SweepViewModel(greenWallet: GreenWallet, privateKey: String?, accountAssetOrNull: AccountAsset?) :
    SweepViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override val privateKey: MutableStateFlow<String> = MutableStateFlow(privateKey ?: "")

    private val _amount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amount: StateFlow<String?> = _amount.asStateFlow()

    private val _amountFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amountFiat: StateFlow<String?> = _amountFiat.asStateFlow()

    override val isWatchOnly: StateFlow<Boolean> = MutableStateFlow(false)

    override val accounts: StateFlow<List<AccountAssetBalance>> = session.accounts.map { accounts ->
        accounts.filter { it.isBitcoin }.map { it.accountAssetBalance }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    private val network
        get() = _network.value!!


    class LocalEvents {
        data class SetPrivateKey(val privateKey: String, val isScan: Boolean) : Event, Redact
    }

    init {

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_sweep)
            )
        }

        session.ifConnected {
            if(accountAsset.value == null) {
                // Set first account asset
                accountAsset.value = session.accounts.value.find { it.isBitcoin }?.accountAsset
            }

            accountAsset.filterNotNull().onEach {
                _network.value = it.account.network
            }.launchIn(this)

            combine(accountAsset, this.privateKey, _feePriorityPrimitive, _feeEstimation) { accountAsset, privateKey, _, _ ->
                _showFeeSelector.value = accountAsset != null
                        && privateKey.isNotBlank()
                        && (accountAsset.account.network.isBitcoin || (accountAsset.account.network.isLiquid && getFeeRate(FeePriority.High()) > accountAsset.account.network.defaultFee))

                createTransactionParams.value = tryCatch(context = Dispatchers.Default) { createTransactionParams() }
            }.launchIn(this)
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SetPrivateKey) {
            privateKey.value = event.privateKey
            _addressInputType = if (event.isScan) AddressInputType.SCAN else AddressInputType.SCAN
        } else if (event is CreateTransactionViewModelAbstract.LocalEvents.SignTransaction) {
            signAndSendTransaction(
                params = createTransactionParams.value,
                originalTransaction = createTransaction.value,
                segmentation = TransactionSegmentation(
                    transactionType = TransactionType.SWEEP,
                    addressInputType = _addressInputType
                ),
                broadcast = event.broadcastTransaction
            )
        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams? {
        if (privateKey.value.isBlank()) {
            _error.value = null
            return null
        }

        // In case of invalid private key, create the CreateTransactionParams
        // there is another check in createTransaction
        val unspentOutputs = try {
            session.getUnspentOutputs(network, privateKey = privateKey.value.trim())
        } catch (e: Exception) {
            e.printStackTrace()
            _error.value = e.message
            null
        }

        return AddressParams(
            address = session.getReceiveAddress(account).address,
            satoshi = 0,
            isGreedy = true
        ).let { params ->
            CreateTransactionParams(
                feeRate = getFeeRate(),
                privateKey = privateKey.value.trim(),
                addressees = listOf(params).toJsonElement(),
                utxos = unspentOutputs?.unspentOutputs
            )
        }
    }

    override fun createTransaction(
        params: CreateTransactionParams?,
        finalCheckBeforeContinue: Boolean
    ) {
        doAsync({
            if (params == null) {
                _amount.value = null
                _amountFiat.value = null
                return@doAsync null
            }

            // Check if this is an error from createTransactionParams
            if (params.utxos.isNullOrEmpty()) {
                throw Exception(_error.value ?: "UTXOs are empty")
            }

            val network = _network.value!!
            val tx = session.createTransaction(network, params)

            val receiveAddress = params.addresseesAsParams?.firstOrNull()?.address
            (tx.outputs.firstOrNull()?.takeIf { it.address == receiveAddress }?.satoshi?.takeIf { it > 0L } ?: // from outputs
            tx.addressees.firstOrNull()?.takeIf { it.address == receiveAddress }?.satoshi?.takeIf { it > 0L } ?: // from addresses
            tx.satoshi[network.policyAsset]) // from satoshi
            .also { amount ->
                _amount.value = amount.toAmountLook(
                    session = session,
                    withUnit = true
                )

                _amountFiat.value = amount.toAmountLook(
                    session = session,
                    withUnit = true,
                    denomination = Denomination.fiat(session)
                )
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

    companion object: Loggable()
}

class SweepViewModelPreview(greenWallet: GreenWallet) :
    SweepViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = previewAccountAsset()) {

    override val accounts: StateFlow<List<AccountAssetBalance>> = MutableStateFlow(listOf(previewAccountAssetBalance(), previewAccountAssetBalance()))
    override val privateKey: MutableStateFlow<String> = MutableStateFlow("privatekey")
    override val amount: StateFlow<String?> = MutableStateFlow("1.0 BTC")
    override val amountFiat: StateFlow<String?> = MutableStateFlow("150.000 USD")
    override val isWatchOnly: StateFlow<Boolean> = MutableStateFlow(false)

    init {
        _feePriority.value = FeePriority.Low(fee = "0.000001 BTC", feeFiat = "13.00 USD", feeRate = 2L.feeRateWithUnit(), expectedConfirmationTime = "id_s_hours|2")
    }

    companion object {
        fun preview() = SweepViewModelPreview(previewWallet())
    }
}
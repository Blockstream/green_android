package com.blockstream.common.models.send

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
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    abstract val accounts: StateFlow<List<Account>>

    @NativeCoroutinesState
    abstract val privateKey: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val amountFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val error: StateFlow<String?>
}

class SweepViewModel(greenWallet: GreenWallet, privateKey: String?, accountAssetOrNull: AccountAsset?) :
    SweepViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override val privateKey: MutableStateFlow<String> = MutableStateFlow(privateKey ?: "")

    private val _amount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amount: StateFlow<String?> = _amount.asStateFlow()

    private val _amountFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amountFiat: StateFlow<String?> = _amountFiat.asStateFlow()

    override val accounts: StateFlow<List<Account>> = session.accounts.map { accounts ->
        accounts.filter { it.isBitcoin }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val checkTransactionMutex = Mutex()

    private val network
        get() = _network.value!!


    class LocalEvents {
        data class SetPrivateKey(val privateKey: String, val isScan: Boolean) : Event, Redact
        data class SignTransaction(
            val broadcastTransaction: Boolean = true
        ) : Event
    }

    init {
        _navData.value = NavData(
            title = "id_sweep"
        )

        session.ifConnected {
            if(accountAsset.value == null) {
                // Set first account asset
                accountAsset.value = session.accounts.value.find { it.isBitcoin }?.accountAsset
            }

            accountAsset.filterNotNull().onEach {
                _network.value = it.account.network
            }.launchIn(this)

            var job: Job? = null
            combine(accountAsset, this.privateKey, _feePriorityPrimitive) { _, _, _ ->
                job?.cancel()
                job = checkTransaction()
            }.launchIn(this)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SetPrivateKey) {
            privateKey.value = event.privateKey
            _addressInputType = if (event.isScan) AddressInputType.SCAN else AddressInputType.SCAN
        } else if (event is LocalEvents.SignTransaction) {
            signTransaction(event.broadcastTransaction)
        }
    }

    private suspend fun createTransactionParams(): CreateTransactionParams {
        logger.d { "createTransactionParams" }
        val unspentOutputs =
            session.getUnspentOutputs(network, privateKey = privateKey.value.trim())

        if(unspentOutputs.unspentOutputs.all { it.value.isEmpty() }){
            throw Exception("id_no_utxos_found")
        }

        return listOf(
            session.getReceiveAddress(account)
        ).let { params ->
            CreateTransactionParams(
                feeRate = getFeeRate(),
                privateKey = privateKey.value.trim(),
                passphrase = "",
                addressees = params.map { it.toJsonElement() },
                addresseesAsParams = params.map {
                    AddressParams(
                        address = it.address,
                        satoshi = 0,
                        isGreedy = true
                    )
                },
                utxos = unspentOutputs.unspentOutputsAsJsonElement
            ).also {
                _transactionParams = it
            }
        }
    }

    private suspend fun checkTransaction(): Job {
        logger.d { "checkTransaction" }
        checkTransactionMutex.withLock {
            return doAsync({
                if (privateKey.value.isBlank()) {
                    _amount.value = null
                    _amountFiat.value = null
                    _isValid.value = false
                    // Error null
                    null
                } else {
                    _network.value?.let { network ->
                        val params = createTransactionParams()
                        val tx = session.createTransaction(network, params)

                        tx.satoshi[network.policyAsset].let { amount ->
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
                            _feePriority.value = calculateFeePriority(session = session, feePriority = _feePriority.value, feeAmount = it, feeRate = tx.feeRate?.feeRateWithUnit())
                        }

                        tx.error.takeIf { it.isNotBlank() }?.also {
                            throw Exception(it)
                        }

                        tx
                    }
                }
            }, preAction = {
                _isValid.value = false
            }, postAction = {

            }, onSuccess = {
                _isValid.value = it != null
                _error.value = null
            }, onError = {
                _isValid.value = false
                _error.value = it.message
            })
        }
    }

    private fun signTransaction(broadcastTransaction: Boolean) {
        doAsync({
            countly.startSendTransaction()
            countly.startFailedTransaction()

            val params = createTransactionParams()
            val signedTransaction = session.createTransaction(network, params).let { tx ->
                tx.error.takeIf { it.isNotBlank() }?.also {
                    throw Exception(it)
                }
                session.signTransaction(account.network, tx)
            }

            if (broadcastTransaction) {
                session.broadcastTransaction(network, signedTransaction.transaction ?: "")
            }

            true
        }, postAction = { exception ->
            onProgress.value = exception == null
        }, onSuccess = {
            if (broadcastTransaction) {
                countly.endSendTransaction(
                    session = session,
                    account = account,
                    transactionSegmentation = TransactionSegmentation(
                        transactionType = TransactionType.SWEEP,
                        addressInputType = _addressInputType
                    ),
                    withMemo = false
                )
            }

            postSideEffect(SideEffects.Snackbar("id_transaction_sent"))
            postSideEffect(SideEffects.NavigateToRoot)
        }, onError = {
            postSideEffect(SideEffects.ErrorDialog(it))
            countly.failedTransaction(
                session = session,
                account = account,
                transactionSegmentation = TransactionSegmentation(
                    transactionType = TransactionType.SWEEP,
                    addressInputType = _addressInputType
                ),
                error = it
            )
        })
    }

    companion object: Loggable()
}

class SweepViewModelPreview(greenWallet: GreenWallet) :
    SweepViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = previewAccountAsset()) {

    override val accounts: StateFlow<List<Account>> = MutableStateFlow(listOf(previewAccount(), previewAccount()))
    override val privateKey: MutableStateFlow<String> = MutableStateFlow("privatekey")
    override val amount: StateFlow<String?> = MutableStateFlow("1.0 BTC")
    override val amountFiat: StateFlow<String?> = MutableStateFlow("150.000 USD")
    override val error: StateFlow<String?> = MutableStateFlow("Error")

    init {
        _feePriority.value = FeePriority.Low(fee = "0.000001 BTC", feeFiat = "13.00 USD", feeRate = 2L.feeRateWithUnit(), expectedConfirmationTime = "id_s_hours|2")
    }

    companion object {
        fun preview() = SweepViewModelPreview(previewWallet())
    }
}
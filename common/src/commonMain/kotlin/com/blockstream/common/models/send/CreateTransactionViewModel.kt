package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_sending
import blockstream_green.common.generated.resources.id_signing
import blockstream_green.common.generated.resources.id_transaction_already_confirmed
import blockstream_green.common.generated.resources.id_transaction_sent
import com.blockstream.common.AddressInputType
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ExceptionWithSupportData
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SupportData
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.gdk.FeeBlockHigh
import com.blockstream.common.gdk.FeeBlockLow
import com.blockstream.common.gdk.FeeBlockMedium
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.ProcessedTransactionDetails
import com.blockstream.common.gdk.params.BroadcastTransactionParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.ifNotNull
import com.blockstream.common.utils.toAmountLook
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.getString


abstract class CreateTransactionViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null,
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    internal val _network: MutableStateFlow<Network?> = MutableStateFlow(null)

    internal val _feePriority: MutableStateFlow<FeePriority> = MutableStateFlow(FeePriority.Low())
    @NativeCoroutinesState
    val feePriority: StateFlow<FeePriority> = _feePriority.asStateFlow()

    internal val _feeEstimation: MutableStateFlow<List<Long>?> = MutableStateFlow(null)

    // Used to trigger
    internal val _feePriorityPrimitive: StateFlow<FeePriority> = _feePriority.map { it.primitive() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeePriority.Low())

    internal var _addressInputType: AddressInputType? = null

    internal val _showFeeSelector: MutableStateFlow<Boolean> = MutableStateFlow(false)
    @NativeCoroutinesState
    val showFeeSelector: StateFlow<Boolean> = _showFeeSelector.asStateFlow()

    internal val _onProgressSending: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val onProgressSending: StateFlow<Boolean> = _onProgressSending.asStateFlow()

    // Gets updated when Account assets are updated
    @NativeCoroutinesState
    val accountAssetBalance: StateFlow<AccountAssetBalance?> =
        combine(accountAsset, denomination, _network.flatMapLatest { network ->
            network?.let {
                accountAsset.flatMapLatest { accountAsset ->
                    accountAsset?.let { sessionOrNull?.accountAssets(it.account) } ?: flowOf(null)
                }
            } ?: flowOf(null)
        }) { accountAsset, denomination, assets ->
            assets
        }.map { assets,  ->
            assets?.let {
                accountAsset.value?.let {
                    AccountAssetBalance.create(
                        accountAsset = it,
                        session = session,
                        denomination = denomination.value
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), accountAssetOrNull?.accountAssetBalance)

    internal var _customFeeRate: MutableStateFlow<Double?> = MutableStateFlow(null)

    @NativeCoroutinesState
    val customFeeRate: StateFlow<Double?> = _customFeeRate.asStateFlow()

    val note = MutableStateFlow(sessionOrNull?.pendingTransaction?.transaction?.memo ?: "")

    protected val _error: MutableStateFlow<String?> = MutableStateFlow(null)

    @NativeCoroutinesState
    val error: StateFlow<String?> = _error.asStateFlow()

    internal var createTransactionParams: MutableStateFlow<CreateTransactionParams?> = MutableStateFlow(null)
    internal var createTransaction: MutableStateFlow<CreateTransaction?> = MutableStateFlow(null)

    internal val createTransactionMutex = Mutex()

    open suspend fun createTransactionParams(): CreateTransactionParams? = null

    open fun createTransaction(params: CreateTransactionParams?, finalCheckBeforeContinue: Boolean = false) { }

    class LocalEvents {
        data class ClickFeePriority(val showCustomFeeRateDialog: Boolean = false) : Event
        data class SetFeeRate(val feePriority: FeePriority) : Event
        data class SetCustomFeeRate(val amount: String) : Event
        data class SetAddressInputType(val inputType: AddressInputType): Event
        data class SignTransaction(val broadcastTransaction: Boolean = true, val createPsbt: Boolean = false) : Event
        data class BroadcastTransaction(val broadcastTransaction: Boolean = true, val psbt: String) : Event
    }

    class LocalSideEffects {
        data class ShowCustomFeeRate(val feeRate: Double) : SideEffect
    }

    override fun bootstrap() {
        session.ifConnected {
            _network.onEach {
                if (it == null) {
                    _feeEstimation.value = null
                } else {
                    _feeEstimation.value = session.getFeeEstimates(it).fees
                    _customFeeRate.value =  ((_feeEstimation.value?.firstOrNull()
                        ?: it.defaultFee) / 1000.0)
                }
            }.launchIn(this)

            combine(
                createTransactionParams,
                denomination,
                merge(flowOf(Unit), session.accountsAndBalanceUpdated), // there is a case where params are equal (lightning), so we need to re-create the transaction
            ) { createTransactionParams, _, _ ->
                createTransaction(params = createTransactionParams, finalCheckBeforeContinue = false)
            }.launchIn(this)
        }

        super.bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SetFeeRate) {
            if (event.feePriority is FeePriority.Custom && event.feePriority.customFeeRate.isNaN()) {

                // Prevent replacing if rate is the same as it will clear the fee estimation data
                FeePriority.Custom(_customFeeRate.value ?: minFee()).also {
                    if(_feePriorityPrimitive.value != it.primitive()){
                        _feePriority.value = it
                    }
                }
                showCustomFeeRateDialog()
            } else {
                _feePriority.value = event.feePriority
            }
        } else if (event is LocalEvents.ClickFeePriority) {
            ifNotNull(accountAsset.value, createTransactionParams.value) { acc, params ->
                if(event.showCustomFeeRateDialog && feePriority.value is FeePriority.Custom){
                    showCustomFeeRateDialog()
                }else{
                    postSideEffect(
                        SideEffects.OpenFeeBottomSheet(
                            greenWallet = greenWallet,
                            accountAsset = acc,
                            params = params
                        )
                    )
                }
            }
        } else if (event is LocalEvents.SetCustomFeeRate) {
            setCustomFeeRate(event.amount)
        } else if (event is LocalEvents.SetAddressInputType) {
            _addressInputType = event.inputType
        }
    }

    internal open fun minFee(): Double =
        (_feeEstimation.value?.firstOrNull() ?: _network.value?.defaultFee
        ?: session.defaultNetwork.defaultFee) / 1000.0

    private fun setCustomFeeRate(amount: String? = null) {
        val minFee = minFee()

        if (amount == null) {
            _customFeeRate.value = minFee
        } else {
            (amount.toDoubleOrNull() ?: 0.0).also {
                if (it < minFee) {
                    postSideEffect(SideEffects.ErrorSnackbar(Exception("id_fee_rate_must_be_at_least_s|$minFee")))
                } else {
                    _customFeeRate.value = it
                }
            }
        }

        // Prevent replacing if rate is the same as it will clear the fee estimation data
        FeePriority.Custom(_customFeeRate.value ?: minFee).also {
            if(_feePriorityPrimitive.value != it.primitive()){
                _feePriority.value = it
            }
        }
    }

    internal suspend fun calculateFeePriority(
        session: GdkSession,
        feePriority: FeePriority,
        feeAmount: Long? = null,
        feeRate: String? = null,
        error: String? = null
    ): FeePriority {
        logger.d { "calculateFeePriority" }
        val fee = feeAmount.toAmountLook(session = session, withUnit = true)
        val feeFiat = feeAmount.toAmountLook(
            session = session,
            withUnit = true,
            denomination = Denomination.fiat(session)
        )
        val expectedConfirmationTime = _network.value?.let { network ->
            val blocksPerHour = network.blocksPerHour

            getFeeRate(feePriority).let { feeRate ->
                _feeEstimation.value?.withIndex()?.indexOfFirst {
                    // Skip relay value
                    if (it.index == 0) {
                        false
                    } else {
                        feeRate >= it.value
                    }
                }.takeIf { it != -1 } ?: 10
            }.let { blocks ->
                val n =
                    if (blocks % blocksPerHour == 0) blocks / blocksPerHour else blocks * (60 / blocksPerHour)
                when {
                    blocks % blocksPerHour != 0 -> "mins"
                    blocks == blocksPerHour -> "h"
                    else -> "h"
                }.let {
                    "~ $n $it"
                }
            }
        }

        return when (feePriority) {
            is FeePriority.Custom -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )

            is FeePriority.High -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )

            is FeePriority.Low -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )

            is FeePriority.Medium -> feePriority.copy(
                fee = fee,
                feeFiat = feeFiat,
                feeRate = feeRate,
                error = error,
                expectedConfirmationTime = expectedConfirmationTime
            )
        }
    }

    private fun showCustomFeeRateDialog() {
        postSideEffect(LocalSideEffects.ShowCustomFeeRate((_customFeeRate.value ?: 0.0).coerceAtLeast(minFee())))
    }

    internal fun getFeeRate(priority: FeePriority = feePriority.value): Long =
        priority.let {
            if (it is FeePriority.Custom) {
                (it.customFeeRate.coerceAtLeast(minFee()) * 1000).toLong()
            } else {
                when (it) {
                    is FeePriority.High -> FeeBlockHigh
                    is FeePriority.Medium -> FeeBlockMedium
                    else -> FeeBlockLow
                }.let { index ->
                    _feeEstimation.value?.getOrNull(index)
                } ?: _network.value?.defaultFee?.coerceAtLeast(
                    _feeEstimation.value?.getOrNull(0) ?: 0
                )
                ?: 0
            }
        }

    private suspend fun transactionConfirmLook() = session.pendingTransaction?.let {
        TransactionConfirmLook.create(
            params = it.params,
            transaction = it.transaction,
            account = account,
            session = session,
            isAddressVerificationOnDevice = true
        )
    }

    internal fun signAndSendTransaction(
        params: CreateTransactionParams?,
        originalTransaction: CreateTransaction?,
        psbt: String? = null,
        segmentation: TransactionSegmentation,
        broadcast: Boolean,
        createPsbt: Boolean = false
    ) {
        doAsync({
            if(params == null || originalTransaction == null){
                throw Exception("No params/transaction is provided")
            }

            onProgressDescription.value = getString(if(broadcast) Res.string.id_sending else Res.string.id_signing)

            if(broadcast) {
                countly.startSendTransaction()
                countly.startFailedTransaction()
            }

            val network = accountOrNull?.network ?: _network.value!!

            // broadcast is handled with simulateOnly flag
            if (psbt != null) {
                return@doAsync session.broadcastTransaction(
                    network = network,
                    broadcastTransaction = BroadcastTransactionParams(
                        psbt = psbt,
                        simulateOnly = !broadcast
                    )
                )
            }

            var transaction = originalTransaction

            val isSwap = transaction.isSwap()

            // If liquid, blind the transaction before signing
            if(network.isLiquid && !transaction.isBump() && !transaction.isSweep()){
                transaction = session.blindTransaction(network, transaction)
            }

            if (!transaction.isSweep() && (session.isWatchOnly.value || createPsbt)) {
                // Create PSBT
                ProcessedTransactionDetails(psbt = session.psbtFromJson(account.network, transaction).psbt)
            } else {
                if (session.isHardwareWallet && !account.isLightning && !transaction.isSweep() && !session.isWatchOnlyValue) {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.DeviceInteraction(
                                greenWalletOrNull = greenWalletOrNull,
                                deviceId = sessionOrNull?.device?.connectionIdentifier,
                                transactionConfirmLook = transactionConfirmLook()
                            )
                        )
                    )
                }

                if(!isSwap) {
                    // Sign transaction
                    transaction = session.signTransaction(account.network, transaction)
                }

                // Broadcast or just sign
                if (broadcast) {
                    if (isSwap || transaction.isSweep()) {
                        session.broadcastTransaction(
                            network = network,
                            broadcastTransaction = BroadcastTransactionParams(
                                transaction = transaction.transaction ?: "",
                                memo = note.value.takeIf { it.isNotBlank() }?.trim() ?: ""
                            )
                        )
                    } else {

                        // Set memo without recreating the transaction
                        val signedTransaction =
                            JsonObject(transaction.jsonElement!!.jsonObject.toMutableMap().apply {
                                this["memo"] =
                                    JsonPrimitive(note.value.takeIf { it.isNotBlank() }?.trim()
                                        ?: ""
                                    )
                            })

                        session.sendTransaction(
                            account = account,
                            signedTransaction = signedTransaction,
                            isSendAll = transaction.isSendAll,
                            isBump = transaction.isBump(),
                            twoFactorResolver = this
                        )
                    }
                } else {
                    ProcessedTransactionDetails(signedTransaction = transaction.transaction ?: "")
                }
            }

        },  preAction = {
            onProgress.value = true
            _onProgressSending.value = true
        }, postAction = {
            onProgress.value = broadcast && it == null
            _onProgressSending.value = broadcast && it == null
        }, onSuccess = {
            // Dismiss Verify Transaction Dialog
            postSideEffect(SideEffects.Dismiss)

            if (it.psbt != null && it.txHash == null) {
                onProgress.value = false
                _onProgressSending.value = false
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.JadeQR(
                            greenWalletOrNull = greenWalletOrNull,
                            operation = JadeQrOperation.Psbt(
                                psbt = it.psbt,
                                transactionConfirmLook = transactionConfirmLook(),
                                askForJadeUnlock = true
                            ),
                            deviceModel = session.deviceModel
                        )
                    )
                )
            } else if (broadcast) {
                countly.endSendTransaction(
                    session = session,
                    account = account,
                    transactionSegmentation = segmentation,
                    withMemo = note.value.isNotBlank()
                )

                session.pendingTransaction = null // clear pending transaction
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_transaction_sent)))
                postSideEffect(SideEffects.NavigateToRoot())
            } else {
                postSideEffect(
                    SideEffects.Dialog(
                        title = StringHolder.create("Signed Transaction"),
                        message = StringHolder.create(it.signedTransaction ?: it.transaction)
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
                            supportData = SupportData.create(
                                throwable = it,
                                network = account.network,
                                session = session
                            )
                        )
                    )
                }

                it.message == "id_transaction_already_confirmed" -> {
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_transaction_already_confirmed)))
                    postSideEffect(SideEffects.NavigateToRoot())
                }

                it.message != "id_action_canceled" -> {
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
                }
            }

            countly.failedTransaction(
                session = session,
                account = account,
                transactionSegmentation = segmentation,
                error = it
            )
        })
    }

    companion object : Loggable()
}
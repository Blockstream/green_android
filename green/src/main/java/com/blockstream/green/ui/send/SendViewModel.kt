package com.blockstream.green.ui.send;

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.blockstream.common.AddressInputType
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.FeeBlockTarget
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Assets
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.lightning.lnUrlPayDescription
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.common.utils.UserInput
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.lnUrlPayBitmap
import com.blockstream.green.ui.bottomsheets.DenominationListener
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.feeRateWithUnit
import com.blockstream.green.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import mu.KLogging
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.math.absoluteValue

@KoinViewModel
class SendViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam accountAsset: AccountAsset,
    @InjectedParam val isSweep: Boolean,
    @InjectedParam address: String?,
    @InjectedParam addressType: AddressInputType?,
    @InjectedParam val bumpTransaction: JsonElement?,
) : AbstractAssetWalletViewModel(wallet, accountAsset), DenominationListener {
    override val filterSubAccountsWithBalance = true

    val isBump = bumpTransaction != null
    val isBumpOrSweep = isBump || isSweep

    var activeRecipient = 0

    private val recipients = MutableLiveData(mutableListOf(
        AddressParamsLiveData.create(
            session = session,
            index = 0,
            address = address,
            addressInputType = addressType,
            accountAsset = _accountAssetLiveData
        )
    ))
    fun getRecipientsLiveData() = recipients

    fun getRecipientLiveData(index: Int) = recipients.value?.getOrNull(index)

    val feeSlider = MutableLiveData<Float>() // SliderHighIndex.toFloat() // fee slider selection, 0 for custom
    val feeAmount = MutableLiveData("") // total tx fee
    val feeAmountFiat = MutableLiveData("") // total tx fee in fiat
    val feeAmountRate = MutableLiveData("") // fee rate

    // fee rate from sharedPreferences only for bitcoin
    var customFee: Long? = null

    var feeRate : Long? =  null
    var feeEstimation: List<Long>? = null

    private var checkedTransaction: CreateTransaction? = null
    val transactionError: MutableLiveData<String?> = MutableLiveData("") // empty string as an initial error to disable next button

    val handledGdkErrors: List<String> = listOf(
        "id_invalid_private_key",
        "id_invalid_address",
        "id_invalid_amount",
        "id_invalid_asset_id",
        "id_invoice_expired",
        "id_amount_must_be_at_least_s",
        "id_amount_must_be_at_most_s"
    ) + listOfNotNull(if(!isBump) "id_insufficient_funds" else null) // On Bump, show fee error on errorTextView

    private val checkTransactionMutex = Mutex()

    init {
        // Update fee estimation on network change
        accountAssetLiveData.asFlow().map {
            it.account.network
        }.distinctUntilChanged().onEach {
            updateFeeEstimation()

            // Set initial fee slider value
            if(feeSlider.value == null){
                feeSlider.value = SliderHighIndex.toFloat()
            }

            session.getSettings(network)?.let {
                FeeBlockTarget
                    .indexOf(it.requiredNumBlocks)
                    .takeIf { it > -1 }?.let {
                        feeSlider.value = 3 - it.toFloat()
                    }
            }

        }.launchIn(viewModelScope.coroutineScope)

        _accountAssetLiveData.asFlow().distinctUntilChanged().onEach {
            setAccountAsset(0, it)
        }.launchIn(viewModelScope.coroutineScope)

        // Check transaction if we get a network event
        // we may have gotten an error "session is required"
        // TODO CHANGE THIS TO SUPPORT MULTI NETWORKS
        session.defaultNetworkOrNull?.also {
            session
                .networkEvents(it).filterNotNull()
                .onEach { event ->
                    if (event.isConnected) {
                        checkTransaction()
                    }
                }.launchIn(viewModelScope.coroutineScope)
        }


        // Fee Slider
        feeSlider
            .asFlow()
            .drop(1) // drop initial value
            .distinctUntilChanged()
            .onEach {
                if(it.toInt() != SliderCustomIndex){
                    feeRate = feeEstimation?.getOrNull(FeeBlockTarget[3 - (it.toInt())])
                }

                checkTransaction()
            }
            .launchIn(viewModelScope.coroutineScope)

        recipients.value?.getOrNull(0)?.let {
            setupChangeObserve(it)
        }
    }

    fun createTransactionSegmentation(): TransactionSegmentation {
        return TransactionSegmentation(
            transactionType = when{
                isSweep -> TransactionType.SWEEP
                isBump -> TransactionType.BUMP
                else -> TransactionType.SEND
            },
            addressInputType = recipients.value?.get(0)?.addressInputType,
            sendAll = isSendAll()
        )
    }

    private fun getBumpTransactionFeeRate(): Long? {
        return bumpTransaction?.jsonObject?.get("fee_rate")?.jsonPrimitive?.longOrNull
    }

    private fun updateFeeEstimation() {
        feeRate = null // reset fee rate

        doUserAction({
            logger.info { "updateFeeEstimation for ${network.id}" }
            session.getFeeEstimates(network)
        }, preAction = null, postAction = null, onSuccess = {
            feeEstimation = if(isBump){

                // Old fee rate + minimum relay
                val bumpFeeAndRelay = (getBumpTransactionFeeRate() ?: it.fees[0]) + (it.minimumRelayFee ?: network.defaultFee)
                it.fees.mapIndexed { index, fee ->
                    if(index == 0) {
                        fee
                    } else {
                        fee.coerceAtLeast(bumpFeeAndRelay)
                    }
                }
            }else{
                it.fees
            }

            // skip if custom fee is selected
            if (feeSlider.value?.toInt() != SliderCustomIndex) {
                // update based on current slider selection
                feeRate = feeEstimation?.getOrNull(FeeBlockTarget[3 - (feeSlider.value ?: SliderHighIndex).toInt()])

                // Update fee
                checkTransaction()
            }
        })
    }

    private fun setupChangeObserve(addressParamsLiveData: AddressParamsLiveData) {

        // Pre select asset
//        assetsLiveData.value?.let { balances ->
//            if (balances.size == 1) {
//                if (addressParamsLiveData.accountAsset.value.isNullOrBlank()) {
//                    addressParamsLiveData.assetId.value = balances.keys.first()
//                }
//            }
//        }

        // Address
        addressParamsLiveData.address
            .asFlow()
            .drop(1)// drop initial value
            .distinctUntilChanged()
            .filterNot { isBump }
            .debounce(50)
            .onEach {
                checkTransaction()
            }
            .launchIn(viewModelScope.coroutineScope)

        // Account
        addressParamsLiveData.accountAsset
            .asFlow()
            .drop(1)// drop initial value
            .distinctUntilChanged()
            .onEach {
                checkTransaction()
            }
            .launchIn(viewModelScope.coroutineScope)

        // Amount
        addressParamsLiveData.amount
            .asFlow()
            .drop(1)// drop initial value
            .distinctUntilChanged()
            .debounce(100) // debounce as user types
            .onEach {
                // Skip if is a bip21 field or is Send all or sweep or bump or Bolt11
                if (
                    addressParamsLiveData.isSendAll.value == false
                    && addressParamsLiveData.amountBip21.value == false
                    && !isBumpOrSweep
                    && !addressParamsLiveData.hasLockedAmount.boolean()
                ) {
                    checkTransaction()
                }

                updateExchange(addressParamsLiveData)
            }
            .launchIn(viewModelScope.coroutineScope)

        // Send All
        addressParamsLiveData.isSendAll
            .asFlow()
            .drop(1)// drop initial value
            .filterNot { isBumpOrSweep }
            .distinctUntilChanged()
            .onEach { isSendAll ->
                // avoid checkTransaction when deselected as the event is fired from the amount field being set to ""
                if(isSendAll) {
                    checkTransaction()
                }
            }
            .launchIn(viewModelScope.coroutineScope)

        if (session.hasLightning) {
            session.lightningNodeInfoStateFlow.drop(1).onEach {
                if (accountAssetValue.account.isLightning) {
                    // Re-check the transaction on node_info update
                    checkTransaction()
                }
            }.launchIn(viewModelScope.coroutineScope)
        }
    }

    private fun updateExchange(addressParamsLiveData: AddressParamsLiveData) {
        addressParamsLiveData.amount.value?.let { amount ->
            // Convert between BTC / Fiat
            doUserAction({
                addressParamsLiveData.accountAsset.value?.assetId?.let { assetId ->
                    if (assetId.isPolicyAsset(network)) {

                        // TODO calculate exchange from string input or from satoshi ?
                        UserInput.parseUserInputSafe(
                            session = session,
                            input = amount,
                            denomination = addressParamsLiveData.denomination.value
                        ).getBalance()?.let {
                            "≈ " + it.toAmountLook(
                                session = session,
                                assetId = assetId,
                                denomination = Denomination.exchange(session, addressParamsLiveData.denomination.value),
                                withUnit = true,
                                withGrouping = true,
                                withMinimumDigits = false
                            )
                        } ?: ""
                    } else {
                        ""
                    }
                }
            }, preAction = null, postAction = null, onSuccess = {
                addressParamsLiveData.exchange.value = it
            }, onError = {
                addressParamsLiveData.exchange.value = ""
            })
        }
    }

    fun addRecipient() {
        recipients.value = recipients.value?.apply { add(AddressParamsLiveData.create(session, size, accountAsset = _accountAssetLiveData)) }
        recipients.value?.lastOrNull()?.let { setupChangeObserve(it) }
    }

    fun removeRecipient(index: Int) {
        recipients.value?.let {
            if (it.size > 1) {
                recipients.value = it.apply { it.removeAt(index) }
                checkTransaction()
            }
        }
    }

    fun getFeeRate(): Long = if(feeSlider.value?.toInt() == SliderCustomIndex){
            // prevent custom fee lower than relay or default fee
            customFee?.coerceAtLeast(feeEstimation?.firstOrNull() ?: network.defaultFee) ?: network.defaultFee
        }else{
            feeRate ?: network.defaultFee.coerceAtLeast(feeEstimation?.getOrNull(0) ?: 0)
        }

    private suspend fun createTransactionParams(): CreateTransactionParams {
        if(accountValue.isLightning){
            return recipients.value!!.map {
                it.toAddressParams(session = session, isGreedy = false)
            }.let { params ->
                CreateTransactionParams(
                    addressees = params.map { it.toJsonElement() },
                    addresseesAsParams = params
                )
            }
        }

        val unspentOutputs = if(isSweep){
            session.getUnspentOutputs(accountValue.network, recipients.value?.get(0)?.address?.value?.trim() ?: "")
        }else{
            session.getUnspentOutputs(accountValue, isBump)
        }

        return when{
            isBump -> {
                CreateTransactionParams(
                    subaccount = accountValue.pointer,
                    feeRate = getFeeRate(),
                    utxos = unspentOutputs.unspentOutputsAsJsonElement,
                    previousTransaction = bumpTransaction,

                )
            }
            isSweep -> {
                listOf(
                    session.getReceiveAddress(accountValue)
                ).let { params ->
                    CreateTransactionParams(
                        feeRate = getFeeRate(),
                        privateKey = recipients.value?.get(0)?.address?.value?.trim() ?: "", // private key
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
                    )
                }
            }
            else -> {
                recipients.value!!.map {
                    it.toAddressParams(session = session, isGreedy = it.isSendAll.boolean())
                }.let { params ->
                    CreateTransactionParams(
                        subaccount = accountValue.pointer,
                        addressees = params.map { it.toJsonElement() },
                        addresseesAsParams = params,
                        feeRate = getFeeRate(),
                        utxos = unspentOutputs.unspentOutputsAsJsonElement
                    )
                }

            }
         }
    }

    private fun checkTransaction(userInitiated: Boolean = false, finalCheckBeforeContinue: Boolean = false) {
        logger.info { "checkTransaction" }

        doUserAction({
            // Prevent race condition
            checkTransactionMutex.withLock {

                val params = createTransactionParams()

                val tx = session.createTransaction(network, params)
                var balance: Assets? = null

                if(finalCheckBeforeContinue && !isSweep){
                    balance = session.getBalance(accountValue)
                }

                // Change UI based on the transaction
                recipients.value?.let { recipients ->
                    for(recipient in recipients){
                        val hasLockedAmount = tx.addressees.getOrNull(recipient.index)?.hasLockedAmount == true

                        // If we have BIP21/sweep/bump, update the amounts from GDK side, and disable text input editing
                        recipient.assetBip21.postValue(tx.addressees.getOrNull(recipient.index)?.bip21Params?.hasAssetId == true)
                        recipient.amountBip21.postValue(tx.addressees.getOrNull(recipient.index)?.bip21Params?.hasAmount == true)
                        recipient.domain.postValue(tx.addressees.getOrNull(recipient.index)?.domain ?: "")

                        tx.addressees.getOrNull(recipient.index)?.metadata.also {
                            recipient.description.postValue(it.lnUrlPayDescription() ?: "")
                            recipient.image.postValue(it.lnUrlPayBitmap())
                        }
                        recipient.hasLockedAmount.postValue(hasLockedAmount)

                        recipient.minAmount.postValue(
                            tx.addressees.getOrNull(recipient.index)?.minAmount?.toAmountLook(
                                session = session,
                                withUnit = false
                            )
                        )
                        recipient.maxAmount.postValue(
                            tx.addressees.getOrNull(recipient.index)?.maxAmount?.toAmountLook(
                                session = session,
                                withUnit = true
                            )
                        )

                        tx.addressees.getOrNull(recipient.index)?.let { addressee ->

                            if (network.isLiquid) {
                                addressee.bip21Params?.assetId?.let { assetId ->
                                    recipient.accountAsset.postValue(AccountAsset(recipient.accountAsset.value!!.account, assetId))
                                }
                            }

                            if(isBump){
                                recipient.address.postValue(addressee.address)
                            }

                            // Get amount from GDK if is a BIP21 or isSendAll or Sweep or Bump or Lightning
                            if(addressee.bip21Params?.hasAmount == true || recipient.isSendAll.value == true || isBumpOrSweep || hasLockedAmount){
                                val assetId = addressee.assetId ?: network.policyAsset
                                if(!assetId.isPolicyAsset(network) && recipient.denomination.value?.isFiat == true){
                                    recipient.denomination.postValue(Denomination.default(session))
                                }

                                recipient.amount.postValue(tx.satoshi[assetId]?.absoluteValue?.let { sendAmount ->
                                    // Avoid UI glitches if isSweep and amount is zero (probably error)
                                    if(isSweep && sendAmount == 0L){
                                        ""
                                    }else{
                                        sendAmount.toAmountLook(
                                            session = session,
                                            assetId = assetId,
                                            denomination = recipient.denomination.value,
                                            withUnit = false,
                                            withGrouping = false
                                        )
                                    }
                                })
                            }
                        }
                    }
                }

                // Check if the specified asset in the uri exists in the wallet, we do this check only if it's final
                if(balance != null){
                    for (addressee in tx.addressees) {
                        addressee.assetId?.let { assetId ->
                            if (!balance.containsAsset(assetId)) {
                                throw Exception("id_no_asset_in_this_account")
                            }
                        }
                    }
                }

                checkedTransaction = tx

                feeAmount.postValue(tx.fee?.toAmountLook(session = session, assetId = network.policyAsset, denomination = getRecipientLiveData(0)?.denomination?.value, withUnit = true, withGrouping = true, withMinimumDigits = false) ?: "")
                feeAmountRate.postValue(tx.feeRateWithUnit() ?: "")
                feeAmountFiat.postValue(tx.fee?.toAmountLook(session = session, denomination = Denomination.fiat(session), withUnit = true, withGrouping = true) ?: "")

                if(tx.error.isNotBlank()){
                    throw Exception(tx.error)
                }

                params to tx
            }
        }, postAction = {
            // Avoid UI glitches
            onProgressAndroid.value = finalCheckBeforeContinue
        }, onSuccess = { pair ->
            transactionError.value = null

            if(finalCheckBeforeContinue){
                session.pendingTransaction = pair
                postSideEffect(SideEffects.Navigate())
            }
        }, onError = {
            transactionError.value = (it.cause?.message ?: it.message).let { error ->
                if(recipients.value?.get(0)?.address?.value.isNullOrBlank() && (error == "id_invalid_address" || error == "id_invalid_private_key")){
                    "" // empty error to avoid ui glitches
                }else if(recipients.value?.get(0)?.amount?.value.isNullOrBlank() && !isSendAll() && (error == "id_invalid_amount" || error == "id_insufficient_funds")){
                    "" // empty error to avoid ui glitches
                }else{
                    error
                }
            }

            if (isSweep) {
                recipients.value?.let { recipients ->
                    for (recipient in recipients) {
                        recipient.amount.value = ""
                    }
                }
            }

            if(isBumpOrSweep && userInitiated){
                onError.postValue(ConsumableEvent(it))
            }
        })
    }

    fun confirmTransaction() {
        checkTransaction(finalCheckBeforeContinue = true)
    }

    fun setUri(uri: String) {
        recipients.value?.getOrNull(activeRecipient)?.address?.value = uri
    }

    fun setAddress(index: Int, address: String, inputType: AddressInputType) {
        recipients.value?.getOrNull(index)?.let {
            it.address.value = address
            it.addressInputType = inputType

            if(accountValue.isLightning && address.isBlank()){
                it.amount.value = ""
            }
        }
    }

    fun setAccountAsset(index: Int, accountAsset: AccountAsset) {
        getRecipientLiveData(index)?.let {
            // Clear amount if is new asset
            if (it.accountAsset.value?.assetId != accountAsset.assetId) {
                it.amount.value = ""
            }
            it.isSendAll.value = false
            // reset isFiat as we don't want to have inconsistencies between btc / assets
            if(it.denomination.value?.isFiat == true){
                it.denomination.value = Denomination.default(session)
            }
            it.accountAsset.value = accountAsset
        }
    }

    fun setCustomFeeRate(feeRate : Long?){
        customFee = feeRate ?: network.defaultFee
        feeSlider.value = SliderCustomIndex.toFloat()
        checkTransaction()
    }

    private fun isSendAll(): Boolean {
        return recipients.value?.map {
            it.isSendAll.boolean()
        }?.reduceOrNull { acc, b ->
            acc || b
        } ?: false
    }

    fun sendAll(index: Int, isSendAll : Boolean) {
        getRecipientLiveData(index)?.let { addressParams ->
            addressParams.isSendAll.value = isSendAll
            if(!isSendAll){ // clear amount
                addressParams.amount.value = ""
            }
        }
    }

    companion object : KLogging() {
        const val SliderCustomIndex = 0
        const val SliderHighIndex = 3
    }

    suspend fun getAmountToConvert(): String{
        return getRecipientLiveData(0)?.let { addressParams ->
            // Get value from the transaction object to get the actual send all amount
            if (checkedTransaction?.isSendAll == true) {
                addressParams.accountAsset.value?.assetId?.let { assetId ->
                    checkedTransaction?.let {
                        it.satoshi[assetId]?.absoluteValue
                    }?.toAmountLook(
                        session = session,
                        assetId = assetId,
                        denomination = addressParams.denomination.value,
                        withUnit = false,
                        withMinimumDigits = false,
                        withGrouping = false
                    ) ?: ""
                } ?: ""
            } else {
                addressParams.amount.value ?: ""
            }
        } ?: ""
    }

    override fun setDenomination(denominatedValue: DenominatedValue) {
        getRecipientLiveData(0)?.also {
            it.amount.value = denominatedValue.asInput(session) ?: ""
            it.denomination.value = denominatedValue.denomination
        }
    }
}

data class AddressParamsLiveData constructor(
    val index: Int,
    val address: MutableLiveData<String>,
    var addressInputType: AddressInputType?,
    val accountAsset: MutableLiveData<AccountAsset>,
    val amount: MutableLiveData<String>,
    val denomination: MutableLiveData<Denomination>,
    val isSendAll: MutableLiveData<Boolean> = MutableLiveData(false),
    val hasLockedAmount: MutableLiveData<Boolean> = MutableLiveData(false),
    var minAmount: MutableLiveData<String?> = MutableLiveData(null),
    var maxAmount: MutableLiveData<String?> = MutableLiveData(null),
    val domain: MutableLiveData<String> = MutableLiveData(""),
    val description: MutableLiveData<String> = MutableLiveData(""),
    val image: MutableLiveData<Bitmap?> = MutableLiveData(null),
    val exchange: MutableLiveData<String> = MutableLiveData(""),
    val assetBip21: MutableLiveData<Boolean> = MutableLiveData(false),
    val amountBip21: MutableLiveData<Boolean> = MutableLiveData(false)
) {

    val network: Network
        get() = accountAsset.value!!.account.network

    suspend fun toAddressParams(session: GdkSession, isGreedy: Boolean): AddressParams {

        val satoshi = when {
            isGreedy -> 0
            accountAsset.value?.assetId.isPolicyAsset(session) -> {
                UserInput.parseUserInputSafe(session = session, input = amount.value, denomination = denomination.value)
                    .getBalance()?.satoshi
            }
            else -> {
                UserInput.parseUserInputSafe(session = session, input = amount.value, assetId = accountAsset.value!!.assetId)
                    .getBalance()?.satoshi
            }
        }

        return AddressParams(
            address = address.value ?: "",
            isGreedy = isGreedy,
            assetId = if (accountAsset.value?.account?.network?.isLiquid == true) accountAsset.value?.assetId else null,
            satoshi = satoshi ?: 0
        )
    }

    companion object : KLogging() {
        fun create(session: GdkSession, index: Int, address: String? = null, addressInputType : AddressInputType? = null, accountAsset: MutableLiveData<AccountAsset>) = AddressParamsLiveData(
            index = index,
            address = MutableLiveData(address ?: ""),
            addressInputType = addressInputType,
            accountAsset = accountAsset,
            amount = MutableLiveData(""),
            denomination = MutableLiveData(Denomination.default(session))
        )
    }
}


package com.blockstream.green.ui.wallet;

import android.content.SharedPreferences
import androidx.lifecycle.*
import com.blockstream.gdk.Balances
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.GreenWallet.Companion.FeeBlockTarget
import com.blockstream.gdk.data.CreateTransaction
import com.blockstream.gdk.params.AddressParams
import com.blockstream.gdk.params.BalanceParams
import com.blockstream.gdk.params.CreateTransactionParams
import com.blockstream.green.Preferences
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.*
import com.blockstream.green.utils.*
import com.greenaddress.greenapi.Session
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import mu.KLogging
import java.util.concurrent.atomic.AtomicBoolean


class SendViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    sharedPreferences: SharedPreferences,
    @Assisted wallet: Wallet,
    @Assisted val isSweep: Boolean,
    @Assisted address: String?,
    @Assisted val bumpTransaction: JsonElement?,
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {
    val isBump = bumpTransaction != null
    val isBumpOrSweep = isBump || isSweep

    var activeRecipient = 0

    private val recipients = MutableLiveData(mutableListOf(AddressParamsLiveData.create(index = 0, address = address)))
    fun getRecipientsLiveData() = recipients

    fun getRecipientLiveData(index: Int) = recipients.value?.getOrNull(index)

    private val assets: MutableLiveData<Balances> = MutableLiveData()
    fun getAssetsLiveData(): LiveData<Balances> = assets

    val feeSlider = MutableLiveData<Float>(SliderHighIndex.toFloat()) // fee slider selection, 0 for custom
    val feeAmount = MutableLiveData("") // total tx fee
    val feeAmountFiat = MutableLiveData("") // total tx fee in fiat
    val feeAmountRate = MutableLiveData("") // fee rate

    // fee rate from sharedPreferences only for bitcoin
    var customFee =
        (if (!session.isLiquid) sharedPreferences.getString(Preferences.DEFAULT_FEE_RATE, null)
            ?.toDoubleOrNull()?.times(1000)?.toLong() else null) ?: session.network.defaultFee

    var feeRate : Long? =  null
    var feeEstimation: List<Long>? = null

    private var checkedTransaction: CreateTransaction? = null
    val transactionError: MutableLiveData<String?> = MutableLiveData("") // empty string as an initial error to disable next button

    val handledGdkErrors = listOf("id_insufficient_funds", "id_invalid_private_key", "id_invalid_address", "id_invalid_amount", "id_invalid_asset_id",)

    init {
        updateFeeEstimation()

        session
            .getBalancesObservable()
            .async()
            .subscribe { balances ->
                assets.value = balances

                if (balances.size == 1) {
                    val assetId = balances.keys.first()
                    for (recipient in recipients.value!!) {
                        if (recipient.assetId.value.isNullOrBlank()) {
                            recipient.assetId.value = assetId
                        }
                    }
                }

            }.addTo(disposables)

        // Check transaction if we get a network event
        // we may have gotten an error "session is required"
        session
            .getNetworkEventObservable()
            .async()
            .subscribeBy(
                onNext = { event ->
                    if (event.connected) {
                        checkTransaction()
                    }
                }
            )
            .addTo(disposables)

        session.getSettings()?.let {
            FeeBlockTarget
                .indexOf(it.requiredNumBlocks)
                .takeIf { it > -1 }?.let {
                feeSlider.value = 3 - it.toFloat()
            }
        }

        // Fee Slider
        feeSlider
            .asFlow()
            .drop(1) // drop initial value
            .distinctUntilChanged()
            .onEach {
                if(it.toInt() != SliderCustomIndex){
                    feeRate = feeEstimation?.getOrNull(GreenWallet.FeeBlockTarget[3 - (it.toInt())])
                }

                checkTransaction()
            }
            .launchIn(viewModelScope)

        recipients.value?.getOrNull(0)?.let {
            setupChangeObserve(it)
        }
    }

    private fun getBumpTransactionFeeRate(): Long? {
        return bumpTransaction?.jsonObject?.get("fee_rate")?.jsonPrimitive?.longOrNull
    }

    private fun updateFeeEstimation() {
        session.observable {
            it.getFeeEstimates()
        }
        .retry(1)
        .subscribeBy(
            onSuccess = {
                feeEstimation = if(isBump){

                    // Old fee rate + minimum relay
                    val bumpFeeAndRelay = (getBumpTransactionFeeRate() ?: it.fees[0]) + (it.minimumRelayFee ?: session.network.defaultFee)
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
                if (feeRate == null && feeSlider.value?.toInt() != SliderCustomIndex) {
                    // update based on current slider selection
                    feeRate = feeEstimation?.getOrNull(FeeBlockTarget[3 - (feeSlider.value ?: SliderHighIndex).toInt()])


                    // Update fee
                    checkTransaction()
                }
            },
            onError = {
                onError.postValue(ConsumableEvent(it))
            }
        )
        .addTo(disposables)
    }

    private fun setupChangeObserve(addressParamsLiveData: AddressParamsLiveData) {

        // Pre select asset
        assets.value?.let { balances ->
            if (balances.size == 1) {
                if (addressParamsLiveData.assetId.value.isNullOrBlank()) {
                    addressParamsLiveData.assetId.value = balances.keys.first()
                }
            }
        }

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
            .launchIn(viewModelScope)

        // Asset Id
        addressParamsLiveData.assetId
            .asFlow()
            .drop(1)// drop initial value
            .filter {
                // Skip if is a bip21 field
                addressParamsLiveData.assetBip21.value == false && isBump
            }
            .distinctUntilChanged()
            .onEach {
                checkTransaction()
            }
            .launchIn(viewModelScope)

        // Amount
        addressParamsLiveData.amount
            .asFlow()
            .drop(1)// drop initial value
            .distinctUntilChanged()
            .debounce(200) // debounce as user types
            .onEach {
                // Skip if is a bip21 field or is Send all or sweep or bump
                if (
                    addressParamsLiveData.isSendAll.value == false
                    && addressParamsLiveData.amountBip21.value == false
                    && !isBumpOrSweep
                ) {
                    checkTransaction()
                }

                updateExchange(addressParamsLiveData)
            }
            .launchIn(viewModelScope)

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
            .launchIn(viewModelScope)
    }

    private fun updateExchange(addressParamsLiveData: AddressParamsLiveData) {
        addressParamsLiveData.amount.value?.let { amount ->
            // Convert between BTC / Fiat
            session.observable {
                if (addressParamsLiveData.assetId.value == session.network.policyAsset) {
                    val isFiat = addressParamsLiveData.isFiat.value ?: false

                    // TODO calculate exchange from string input or from satoshi ?


                    UserInput.parseUserInput(
                            session,
                            addressParamsLiveData.amount.value,
                            isFiat = isFiat
                        ).getBalance(session)?.let {
                        "≈ " + if (isFiat) {
                            it.btc(
                                session,
                                withUnit = true,
                                withGrouping = true,
                                withMinimumDigits = false
                            )
                        } else {
                            it.fiat(session, withUnit = true, withGrouping = true)
                        }
                    } ?: ""
                } else {
                    ""
                }
            }.subscribeBy(
                onSuccess = {
                    addressParamsLiveData.exchange.value = it
                },
                onError = {
                    it.printStackTrace()
                    addressParamsLiveData.exchange.value = ""
                }
            )
        }
    }

    fun addRecipient() {
        recipients.value = recipients.value?.apply { add(AddressParamsLiveData.create(size)) }
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

    private fun getFeeRate(): Long = if(feeSlider.value?.toInt() == SliderCustomIndex){
            // prevent custom fee lower than relay or default fee
            customFee.coerceAtLeast(feeEstimation?.firstOrNull() ?: session.network.defaultFee)
        }else{
            feeRate ?: session.network.defaultFee.coerceAtLeast(feeEstimation?.getOrNull(0) ?: 0)
        }

    private fun createTransactionParams(): CreateTransactionParams {
        val unspentOutputs = session.getUnspentOutputs(
            BalanceParams(
                subaccount = wallet.activeAccount,
                confirmations = if(isBump) 1 else 0
            )
        )

        return when{
            isBump -> {
                CreateTransactionParams(
                    subaccount = wallet.activeAccount,
                    feeRate = getFeeRate(),
                    utxos = unspentOutputs.unspentOutputsAsJsonElement,
                    previousTransaction = bumpTransaction,

                )
            }
            isSweep -> {
                CreateTransactionParams(
                    subaccount = wallet.activeAccount,
                    addressees = listOf(AddressParams(address = session.getReceiveAddress(wallet.activeAccount).address)),
                    feeRate = getFeeRate(),
                    privateKey = recipients.value?.get(0)?.address?.value?.trim() ?: "", // private key
                    passphrase = "",
                )
            }
            else -> {
                val isSendAll = isSendAll()
                CreateTransactionParams(
                    subaccount = wallet.activeAccount,
                    addressees = recipients.value!!.map {
                        it.toAddressParams(session = session, isSendAll = isSendAll)
                    },
                    sendAll = isSendAll,
                    feeRate = getFeeRate(),
                    utxos = unspentOutputs.unspentOutputsAsJsonElement
                )
            }
         }
    }

    var pendingCheck = false
    var isCheckingTransaction = AtomicBoolean(false)
    private fun checkTransaction(userInitiated: Boolean = false, finalCheckBeforeContinue: Boolean = false) {
        // Prevent race condition
        if (!isCheckingTransaction.compareAndSet(false, true)){
            pendingCheck = true
            return
        }

        pendingCheck = false

        logger.info { "checkTransaction" }
        session.observable {
            val params = createTransactionParams()

            val tx = it.createTransaction(params)
            var balance: Balances? = null

            if(finalCheckBeforeContinue){
                balance = it.getBalance(
                    BalanceParams(
                        subaccount = wallet.activeAccount,
                        confirmations = 0
                    )
                )
            }

            // Change UI based on the transaction
            recipients.value?.let { recipients ->
                for(recipient in recipients){

                    // If we have BIP21/sweep/bump, update the amounts from GDK side, and disable text input editing
                    recipient.assetBip21.postValue(tx.addressees.getOrNull(recipient.index)?.bip21Params?.hasAssetId == true)
                    recipient.amountBip21.postValue(tx.addressees.getOrNull(recipient.index)?.bip21Params?.hasAmount == true)

                    tx.addressees.getOrNull(recipient.index)?.let { addressee ->

                        if (session.isLiquid) {
                            addressee.bip21Params?.assetId?.let { assetId ->
                                recipient.assetId.postValue(assetId)
                            }
                        }

                        if(isBump){
                            recipient.address.postValue(addressee.address)
                        }

                        // Get amount from GDK if is a BIP21 or isSendAll or Sweep or Bump
                        if(addressee.bip21Params?.hasAmount == true || recipient.isSendAll.value == true || isBumpOrSweep){
                            val assetId = addressee.assetId ?: session.policyAsset
                            if(!assetId.isPolicyAsset(session)){
                                recipient.isFiat.postValue(false)
                            }
                            recipient.amount.postValue(
                                (if (recipient.isSendAll.value == true && session.isElectrum) addressee.satoshi else tx.satoshi[assetId])
                                    ?.toAmountLook(
                                    session = session,
                                    assetId = assetId,
                                    isFiat = recipient.isFiat.value,
                                    withUnit = false
                                )
                            )
                        }
                    }
                }
            }

            // Check if the specified asset in the uri exists in the wallet, we do this check only if it's final
            balance?.let { balance ->
                for (addressee in tx.addressees) {
                    addressee.assetId?.let { assetId ->
                        if (!balance.containsKey(assetId)) {
                            throw Exception("id_no_asset_in_this_account")
                        }
                    }
                }
            }

            if(tx.error.isNotBlank()){
                throw Exception(tx.error)
            }

            tx

        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
            isCheckingTransaction.set(false)
        }
        .doAfterTerminate {
            if(pendingCheck){
                checkTransaction(userInitiated = userInitiated, finalCheckBeforeContinue = finalCheckBeforeContinue)
            }
        }.subscribeBy(
            onSuccess = { tx ->
                checkedTransaction = tx
                transactionError.value = null

                feeAmount.value = tx.fee?.toAmountLook(session, withUnit = true, withGrouping = true, withMinimumDigits = false) ?: ""
                feeAmountRate.value = tx.feeRateWithUnit() ?: ""
                feeAmountFiat.value = tx.fee?.toFiatLook(session = session, withUnit = true, withGrouping = true) ?: ""

                if(!pendingCheck && finalCheckBeforeContinue){
                    Session.getSession().pendingTransaction = tx.toObjectNode()
                    onEvent.postValue(ConsumableEvent(NavigateEvent.Navigate))
                }
            },
            onError = {
                it.printStackTrace()
                transactionError.value = it.cause?.message ?: it.message

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

                feeAmount.value = ""
                feeAmountRate.value = ""
                feeAmountFiat.value = ""
            }
        )
    }

    fun confirmTransaction() {
        checkTransaction(finalCheckBeforeContinue = true)
    }

    fun setBip21Uri(bip21Uri: String) {
        recipients.value?.getOrNull(activeRecipient)?.address?.value = bip21Uri
    }

    fun setAddress(index: Int, address: String) {
        recipients.value?.getOrNull(index)?.address?.value = address
    }

    fun setAsset(index: Int, assetId: String) {
        getRecipientLiveData(index)?.let {
            // Clear amount if is new asset
            if (it.assetId.value != assetId) {
                it.amount.value = ""
            }
            it.isSendAll.value = false
            it.isFiat.value = false // reset isFiat as we don't want to have inconsistencies between btc / assets
            it.assetId.value = assetId
        }
    }

    fun setCustomFeeRate(feeRate : Long?){
        customFee = feeRate ?: session.network.defaultFee
        feeSlider.value = SliderCustomIndex.toFloat()
        checkTransaction()
    }

    private fun isSendAll(): Boolean {
        return recipients.value?.map {
            it.isSendAll.value ?: false
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

    fun toggleCurrency(index: Int) {
        getRecipientLiveData(index)?.let { addressParams ->

            val isFiat = addressParams.isFiat.value ?: false

            // Toggle it first as the amount trigger will be called with wrong isFiat value
            addressParams.isFiat.value = !isFiat

            // Get value from the transaction object to get the actual send all amount
            val amountToConvert = if(checkedTransaction?.isSendAll == true){
                addressParams.assetId.value?.let { assetId ->
                    checkedTransaction?.satoshi?.get(assetId)?.toAmountLook(session, assetId = assetId , withUnit = false, withMinimumDigits = false, withGrouping = false)
                }
            }else{
                addressParams.amount.value ?: ""
            }

            // If isSend All, skip conversion and get the actual value
            if(checkedTransaction?.isSendAll == true && isFiat){
                addressParams.amount.value = amountToConvert
                return
            }

            // Convert between BTC / Fiat
            addressParams.amount.value = try {
                val input =
                    UserInput.parseUserInput(session, amountToConvert, isFiat = isFiat)
                input.getBalance(session)?.let {
                    if (it.satoshi > 0) {
                        if (isFiat) {
                            it.btc(
                                session,
                                withUnit = false,
                                withGrouping = false,
                                withMinimumDigits = false
                            )
                        } else {
                            it.fiat(session, withUnit = false, withGrouping = false)
                        }
                    } else {
                        ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            isSweep: Boolean,
            address: String?,
            bumpTransaction: JsonElement?
        ): SendViewModel
    }

    companion object : KLogging() {
        const val SliderCustomIndex = 0
        const val SliderHighIndex = 3

        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            isSweep: Boolean,
            address: String?,
            bumpTransaction: JsonElement?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, isSweep, address, bumpTransaction) as T
            }
        }
    }
}

data class AddressParamsLiveData constructor(
    val index: Int,
    val address: MutableLiveData<String>,
    val assetId: MutableLiveData<String>,
    val amount: MutableLiveData<String>,
    val isSendAll: MutableLiveData<Boolean> = MutableLiveData(false),
    val exchange: MutableLiveData<String> = MutableLiveData(""),
    val isFiat: MutableLiveData<Boolean> = MutableLiveData(false),
    val assetBip21: MutableLiveData<Boolean> = MutableLiveData(false),
    val amountBip21: MutableLiveData<Boolean> = MutableLiveData(false)
) {

    fun toAddressParams(session: GreenSession, isSendAll: Boolean): AddressParams {

        val satoshi = when {
            isSendAll -> 0
            assetId.value == session.network.policyAsset -> {
                UserInput.parseUserInputSafe(session, amount.value, isFiat = isFiat.value ?: false)
                    .getBalance(session)?.satoshi ?: 0
            }
            else -> {
                UserInput.parseUserInputSafe(session, amount.value, assetId = assetId.value)
                    .getBalance(session)?.satoshi ?: 0
            }
        }

        return AddressParams(
            address = address.value ?: "",
            assetId = if (session.isLiquid) assetId.value else null,
            satoshi = satoshi
        )
    }

    companion object : KLogging() {
        fun create(index: Int, address: String? = null) = AddressParamsLiveData(
            index = index,
            address = MutableLiveData(address ?: ""),
            assetId = MutableLiveData(""),
            amount = MutableLiveData("")
        )
    }
}
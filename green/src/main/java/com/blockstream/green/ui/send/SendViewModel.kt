package com.blockstream.green.ui.send;

import androidx.lifecycle.*
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.GdkBridge.Companion.FeeBlockTarget
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.CreateTransaction
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.params.AddressParams
import com.blockstream.gdk.params.CreateTransactionParams
import com.blockstream.green.data.*
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.gdk.*
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import mu.KLogging


class SendViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted accountAsset: AccountAsset,
    @Assisted val isSweep: Boolean,
    @Assisted("address") address: String?,
    @Assisted val bumpTransaction: JsonElement?,
) : AbstractAssetWalletViewModel(sessionManager, walletRepository, countly, wallet, accountAsset) {
    override val filterSubAccountsWithBalance = true

    val isBump = bumpTransaction != null
    val isBumpOrSweep = isBump || isSweep

    var activeRecipient = 0

    private val recipients = MutableLiveData(mutableListOf(
        AddressParamsLiveData.create(
            index = 0,
            address = address,
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

    val handledGdkErrors = listOf("id_insufficient_funds", "id_invalid_private_key", "id_invalid_address", "id_invalid_amount", "id_invalid_asset_id",)

    private val checkTransactionMutex = Mutex()

    init {
        countly.startSendTransaction()

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

        }.launchIn(viewModelScope)

        _accountAssetLiveData.asFlow().distinctUntilChanged().onEach {
            setAccountAsset(0, it)
        }.launchIn(viewModelScope)

        // Check transaction if we get a network event
        // we may have gotten an error "session is required"
        // TODO CHANGE THIS TO SUPPORT MULTI NETWORKS
        session
            .networkEventsFlow(session.defaultNetwork).filterNotNull()
            .onEach {  event ->
                if (event.isConnected) {
                    checkTransaction()
                }

            }.launchIn(viewModelScope)

        // Fee Slider
        feeSlider
            .asFlow()
            .drop(1) // drop initial value
            .distinctUntilChanged()
            .onEach {
                if(it.toInt() != SliderCustomIndex){
                    feeRate = feeEstimation?.getOrNull(GdkBridge.FeeBlockTarget[3 - (it.toInt())])
                }

                checkTransaction()
            }
            .launchIn(viewModelScope)

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
            .launchIn(viewModelScope)

        // Account
        addressParamsLiveData.accountAsset
            .asFlow()
            .drop(1)// drop initial value
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
            doUserAction({
                addressParamsLiveData.accountAsset.value?.assetId?.let { assetId ->
                    if (assetId.isPolicyAsset(network)) {
                        val isFiat = addressParamsLiveData.isFiat.value ?: false

                        // TODO calculate exchange from string input or from satoshi ?
                        UserInput.parseUserInputSafe(
                            session,
                            amount,
                            isFiat = isFiat
                        ).getBalance(session)?.let {
                            "≈ " + it.toAmountLook(
                                session = session,
                                assetId = assetId,
                                isFiat = !isFiat,
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
        recipients.value = recipients.value?.apply { add(AddressParamsLiveData.create(size, accountAsset = _accountAssetLiveData)) }
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

    private fun createTransactionParams(): CreateTransactionParams {
        val unspentOutputs = session.getUnspentOutputs(account, isBump)

        return when{
            isBump -> {
                CreateTransactionParams(
                    subaccount = account.pointer,
                    feeRate = getFeeRate(),
                    utxos = unspentOutputs.unspentOutputsAsJsonElement,
                    previousTransaction = bumpTransaction,

                )
            }
            isSweep -> {
                CreateTransactionParams(
                    feeRate = getFeeRate(),
                    privateKey = recipients.value?.get(0)?.address?.value?.trim() ?: "", // private key
                    passphrase = "",
                    addressees = listOf(AddressParams(
                        address = session.getReceiveAddress(account).address
                    ))
                )
            }
            else -> {
                val isSendAll = isSendAll()
                CreateTransactionParams(
                    subaccount = account.pointer,
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

    // This method helps between differences between multisig/singlesig returned values
    private fun getSendAmountCompat(index: Int, assetId: String, tx: CreateTransaction) = if (network.isSinglesig) tx.addressees[index].satoshi else tx.satoshi[assetId]


    private fun checkTransaction(userInitiated: Boolean = false, finalCheckBeforeContinue: Boolean = false) {
        logger.info { "checkTransaction" }

        var params: CreateTransactionParams? = null

        doUserAction({
            // Prevent race condition
            checkTransactionMutex.withLock {

                params = createTransactionParams()

                val tx = session.createTransaction(network, params!!)
                var balance: Assets? = null

                if(finalCheckBeforeContinue){
                    balance = session.getBalance(account)
                }

                // Change UI based on the transaction
                recipients.value?.let { recipients ->
                    for(recipient in recipients){

                        // If we have BIP21/sweep/bump, update the amounts from GDK side, and disable text input editing
                        recipient.assetBip21.postValue(tx.addressees.getOrNull(recipient.index)?.bip21Params?.hasAssetId == true)
                        recipient.amountBip21.postValue(tx.addressees.getOrNull(recipient.index)?.bip21Params?.hasAmount == true)

                        tx.addressees.getOrNull(recipient.index)?.let { addressee ->

                            if (network.isLiquid) {
                                addressee.bip21Params?.assetId?.let { assetId ->
                                    recipient.accountAsset.postValue(AccountAsset(recipient.accountAsset.value!!.account, assetId))
                                }
                            }

                            if(isBump){
                                recipient.address.postValue(addressee.address)
                            }

                            // Get amount from GDK if is a BIP21 or isSendAll or Sweep or Bump
                            if(addressee.bip21Params?.hasAmount == true || recipient.isSendAll.value == true || isBumpOrSweep){
                                val assetId = addressee.assetId ?: network.policyAsset
                                if(!assetId.isPolicyAsset(network)){
                                    recipient.isFiat.postValue(false)
                                }

                                recipient.amount.postValue(getSendAmountCompat(recipient.index, assetId, tx)?.let {sendAmount ->
                                    // Avoid UI glitches if isSweep and amount is zero (probably error)
                                    if(isSweep && sendAmount == 0L){
                                        ""
                                    }else{
                                        sendAmount.toAmountLook(
                                            session = session,
                                            assetId = assetId,
                                            isFiat = recipient.isFiat.value,
                                            withUnit = false,
                                            withGrouping = false,
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
                            if (!balance.containsKey(assetId)) {
                                throw Exception("id_no_asset_in_this_account")
                            }
                        }
                    }
                }

                checkedTransaction = tx

                feeAmount.postValue(tx.fee?.toAmountLook(session, withUnit = true, withGrouping = true, withMinimumDigits = false) ?: "")
                feeAmountRate.postValue(tx.feeRateWithUnit() ?: "")
                feeAmountFiat.postValue(tx.fee?.toAmountLook(session = session, isFiat = true, withUnit = true, withGrouping = true) ?: "")

                if(tx.error.isNotBlank()){
                    throw Exception(tx.error)
                }

                tx
            }
        }, onSuccess = { tx ->
            transactionError.value = null

            if(finalCheckBeforeContinue){
                session.pendingTransaction = params!! to tx
                onEvent.postValue(ConsumableEvent(NavigateEvent.Navigate))
            }
        }, onError = {
            transactionError.value = (it.cause?.message ?: it.message).let { error ->
                if(recipients.value?.get(0)?.address?.value.isNullOrBlank() && (error == "id_invalid_address" || error == "id_invalid_private_key")){
                    "" // empty error to avoid ui glitches
                }else if(recipients.value?.get(0)?.amount?.value.isNullOrBlank() && (error == "id_invalid_amount" || error == "id_insufficient_funds")){
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

    fun setBip21Uri(bip21Uri: String) {
        recipients.value?.getOrNull(activeRecipient)?.address?.value = bip21Uri
    }

    fun setScannedAddress(index: Int, address: String) {
        recipients.value?.getOrNull(index)?.let {
            it.address.value = address
            it.addressInputType = AddressInputType.SCAN
        }
    }

    fun setAccountAsset(index: Int, accountAsset: AccountAsset) {
        getRecipientLiveData(index)?.let {
            // Clear amount if is new asset
            if (it.accountAsset.value?.assetId != accountAsset.assetId) {
                it.amount.value = ""
            }
            it.isSendAll.value = false
            it.isFiat.value = false // reset isFiat as we don't want to have inconsistencies between btc / assets
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
                addressParams.accountAsset.value?.assetId?.let { assetId ->
                    checkedTransaction?.let {
                        getSendAmountCompat(addressParams.index, assetId, it)
                    }
                    ?.toAmountLook(session = session, assetId = assetId , withUnit = false, withMinimumDigits = false, withGrouping = false)
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
                UserInput
                    .parseUserInput(session, amountToConvert, isFiat = isFiat)
                    .getBalance(session)
                    ?.toAmountLook(
                        session = session,
                        isFiat = !isFiat,
                        withUnit = false,
                        withGrouping = false,
                        withMinimumDigits = false
                    )
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
            accountAsset: AccountAsset,
            isSweep: Boolean,
            @Assisted("address")
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
            accountAsset: AccountAsset,
            isSweep: Boolean,
            address: String?,
            bumpTransaction: JsonElement?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, accountAsset, isSweep, address, bumpTransaction) as T
            }
        }
    }
}

data class AddressParamsLiveData constructor(
    val index: Int,
    val address: MutableLiveData<String>,
    var addressInputType: AddressInputType?,
    val accountAsset: MutableLiveData<AccountAsset>,
    val amount: MutableLiveData<String>,
    val isSendAll: MutableLiveData<Boolean> = MutableLiveData(false),
    val exchange: MutableLiveData<String> = MutableLiveData(""),
    val isFiat: MutableLiveData<Boolean> = MutableLiveData(false),
    val assetBip21: MutableLiveData<Boolean> = MutableLiveData(false),
    val amountBip21: MutableLiveData<Boolean> = MutableLiveData(false)
) {

    val network: Network
        get() = accountAsset.value!!.account.network

    fun toAddressParams(session: GdkSession, isSendAll: Boolean): AddressParams {

        val satoshi = when {
            isSendAll -> 0
            accountAsset.value?.assetId.isPolicyAsset(session) -> {
                UserInput.parseUserInputSafe(session, amount.value, isFiat = isFiat.value ?: false)
                    .getBalance(session)?.satoshi ?: 0
            }
            else -> {
                UserInput.parseUserInputSafe(session, amount.value, assetId = accountAsset.value!!.assetId)
                    .getBalance(session)?.satoshi ?: 0
            }
        }

        return AddressParams(
            address = address.value ?: "",
            assetId = if (accountAsset.value?.account?.network?.isLiquid == true) accountAsset.value?.assetId else null,
            satoshi = satoshi
        )
    }

    companion object : KLogging() {
        fun create(index: Int, address: String? = null, accountAsset: MutableLiveData<AccountAsset>) = AddressParamsLiveData(
            index = index,
            address = MutableLiveData(address ?: ""),
            addressInputType = if(address.isNullOrBlank()) null else AddressInputType.BIP21,
            accountAsset = accountAsset,
            amount = MutableLiveData("")
        )
    }
}
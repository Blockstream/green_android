package com.blockstream.common.models.send

import com.blockstream.common.AddressInputType
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.Banner
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.ExceptionWithErrorReport
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.startsWith
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.lightning.lnUrlPayDescription
import com.blockstream.common.lightning.lnUrlPayImage
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.ifNotNull
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.buildJsonObject
import saschpe.kase64.base64DecodedBytes
import kotlin.math.absoluteValue

abstract class SendViewModelAbstract(greenWallet: GreenWallet) :
    CreateTransactionViewModelAbstract(greenWallet = greenWallet) {
    override fun screenName(): String = "Send"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    @NativeCoroutinesState
    abstract val errorAddress: StateFlow<String?>

    @NativeCoroutinesState
    abstract val errorAmount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val errorGeneric: StateFlow<String?>

    @NativeCoroutinesState
    abstract val assetsAndAccounts: StateFlow<List<AccountAssetBalance>?>

    @NativeCoroutinesState
    abstract val isAccountEdit: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val address: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amountExchange: StateFlow<String>

    @NativeCoroutinesState
    abstract val amountHint: StateFlow<String?>

    @NativeCoroutinesState
    abstract val isAmountLocked: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isSendAll: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val supportsSendAll: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val metadataDomain: StateFlow<String?>

    @NativeCoroutinesState
    abstract val metadataImage: StateFlow<ByteArray?>

    @NativeCoroutinesState
    abstract val metadataDescription: StateFlow<String?>
}

class SendViewModel(
    greenWallet: GreenWallet,
    initAddress: String? = null,
    addressType: AddressInputType? = null
) : SendViewModelAbstract(greenWallet = greenWallet) {

    private val _supportsSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val supportsSendAll: StateFlow<Boolean> = _supportsSendAll.asStateFlow()

    override val isSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val assetId: MutableStateFlow<String?> = MutableStateFlow(null)

    override val assetsAndAccounts = _network.flatMapLatest { network ->
        combine(assetId, _denomination, network?.let {
            session.accountAsset
        } ?: flowOf(null)) { _, _, accountAssets ->
            accountAssets
        }
    }.map {
        ifNotNull(it, _network.value) { accountAssets, network ->
            accountAssets.filter { aa -> aa.account.network.isSameNetwork(network) && assetId.value.let { it == null || it == aa.assetId } }
        }?.mapNotNull {
            AccountAssetBalance.createIfBalance(
                accountAsset = it,
                session = session,
                denomination = denomination.value
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _errorAddress: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorAddress: StateFlow<String?> = _errorAddress.asStateFlow()

    private val _errorAmount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorAmount: StateFlow<String?> = _errorAmount.asStateFlow()

    private val _errorGeneric: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorGeneric: StateFlow<String?> = _errorGeneric.asStateFlow()

    override val address: MutableStateFlow<String> = MutableStateFlow(initAddress ?: "")

    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    override val amountExchange: StateFlow<String> = amount.map { amount ->
        session.ifConnected {
            accountAsset.value?.assetId?.takeIf { it.isPolicyAsset(session) }?.let { assetId ->
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

    private val _metadataDomain: MutableStateFlow<String?> = MutableStateFlow(null)
    override val metadataDomain: StateFlow<String?> = _metadataDomain.asStateFlow()

    private val _metadataImage: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    override val metadataImage: StateFlow<ByteArray?> = _metadataImage.asStateFlow()

    private val _metadataDescription: MutableStateFlow<String?> = MutableStateFlow(null)
    override val metadataDescription: StateFlow<String?> = _metadataDescription.asStateFlow()


    override val isAccountEdit: StateFlow<Boolean> = combine(assetsAndAccounts, accountAsset){ assetsAndAccounts, accountAsset ->
        assetsAndAccounts?.isNotEmpty() == true && accountAsset?.account?.isLightning != true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    class LocalEvents {
        object ToggleIsSendAll: Event
        object SendLightningTransaction : Event
        object ClickAssetsAccounts: Event
    }

    init {
        _addressInputType = addressType

        _navData.value = NavData(title = "id_send", subtitle = greenWallet.name)

        session.ifConnected {

            sessionManager.pendingUri.filterNotNull().onEach {
                sessionManager.pendingUri.value = null
                address.value = it
                postSideEffect(SideEffects.Snackbar("id_address_was_filled_by_a_payment"))
            }.launchIn(this)

            // When changing between accounts, reset isSendAll flag
            accountAsset.onEach {
                isSendAll.value = false
                _supportsSendAll.value = it?.account?.isLightning == false
            }.launchIn(this)

            // When changing between same network but different asset
            combine(accountAsset.map {
                it?.assetId
            }, _network) { _, _ ->
                amount.value = ""
            }.launchIn(this)

            combine(
                address,
                _feeEstimation,
                session.accountAsset,
                accountAsset,
                amount,
                isSendAll,
                _feePriorityPrimitive,
                merge(flowOf(Unit), session.accountsAndBalanceUpdated), // set initial value, watch for wallet balance updates, especially on wallet startup like bip39 uris
            ) { arr ->
                val address = arr[0] as String

                if (address.isBlank()) {
                    // Clear all errors and amount
                    amount.value = ""
                    _isAmountLocked.value = false
                    _error.value = null
                }

                val network =
                    address.takeIf { it.isNotBlank() }?.let { session.parseInput(it)?.first }
                        .also { network ->
                            if (network == null && address.isNotBlank()) {
                                _error.value = "id_invalid_address"
                            }
                        }

                _showFeeSelector.value = (address.isNotBlank()
                        && accountAsset.value != null
                        && (network?.isBitcoin == true || (network?.isLiquid == true && getFeeRate(FeePriority.High()) > network.defaultFee)))

                // Check if the current AccountAsset operates on the same network only.
                // That way we preserve the asset from previous action
                if(network != null && (accountAsset.value?.let { !it.account.network.isSameNetwork(network) || it.balance(session) <= 0} != false)){
                    accountAsset.value = findAccountAsset(network, assetId = assetId.value ?: network.policyAsset)
                }

                // Prefer the real network from the account
                _network.value = network?.let { accountAsset.value?.account?.network } ?: network
            }.onEach {
                createTransactionParams.value = createTransactionParams()
            }.launchIn(this)

            error.onEach {
                _errorAddress.value = it.takeIf { listOf("id_invalid_address", "id_invoice_expired").contains(it) }
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
                }
                _errorGeneric.value = it.takeIf { _errorAmount.value.isNullOrBlank() && _errorAddress.value.isNullOrBlank() }
            }.launchIn(this)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is LocalEvents.ClickAssetsAccounts){
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.AssetsAccounts(
                        greenWallet = greenWallet,
                        assetsAccounts = assetsAndAccounts.value ?: listOf()
                    )
                )
            )
        } else if(event is LocalEvents.ToggleIsSendAll){
            isSendAll.value = isSendAll.value.let { isSendAll ->
                if(isSendAll){
                    amount.value = ""
                }
                !isSendAll
            }
        } else if (event is Events.Continue) {
            createTransactionParams.value?.also {
                createTransaction(params = it, finalCheckBeforeContinue = true)
            }
        } else if(event is LocalEvents.SendLightningTransaction){
            sendLightningTransaction()
        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams? {
        if (address.value.isBlank() || _network.value == null) {
            return null
        }

        return (if (accountAsset.value?.account?.network?.isLightning == true) {
            val satoshi = UserInput.parseUserInputSafe(
                session = session,
                input = amount.value,
                denomination = denomination.value
            ).getBalance(onlyInAcceptableRange = false)?.satoshi

            AddressParams(
                address = address.value,
                satoshi = satoshi ?: 0
            ).let { params ->
                CreateTransactionParams(
                    addressees = listOf(params.toJsonElement()),
                    addresseesAsParams = listOf(params),
                    utxos = buildJsonObject {
                        // a hack to re-create params when balance changes
                        session.accountAssets(account).value.policyAsset
                    }
                )
            }
        } else {
            val isGreedy = isSendAll.value
            val satoshi = if (isGreedy) 0 else UserInput.parseUserInputSafe(
                session = session,
                input = amount.value,
                assetId = accountAsset.value?.assetId,
                denomination = denomination.value
            ).getBalance(onlyInAcceptableRange = false)?.satoshi

            val unspentOutputs = accountAsset.value?.account?.let { session.getUnspentOutputs(it) }

            AddressParams(
                address = address.value,
                satoshi = satoshi ?: 0,
                isGreedy = isGreedy,
                assetId = accountAsset.value?.assetId?.takeIf { account.network.isLiquid }
            ).let { params ->
                CreateTransactionParams(
                    from = accountAsset.value,
                    addressees = listOf(params.toJsonElement()),
                    addresseesAsParams = listOf(params),
                    feeRate = getFeeRate(),
                    utxos = unspentOutputs?.unspentOutputsAsJsonElement
                )
            }
        }).also {
            createTransactionParams.value = it
        }
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
                return@doAsync null
            }

            accountAsset.value?.let { accountAsset ->
                val network = accountAsset.account.network

                val tx = session.createTransaction(network, params)

                // Clear error as soon as possible
                if (tx.error.isBlank()) {
                    _error.value = null
                }

                tx.addressees.firstOrNull()?.also { addressee ->
                    _isAmountLocked.value = addressee.isAmountLocked == true

                    addressee.bip21Params?.assetId?.let { assetId ->
                        this.assetId.value = assetId
                        this.accountAsset.value = findAccountAsset(network = network, assetId = assetId)
                    } ?: kotlin.run {
                        assetId.value = null
                    }

                    _metadataDomain.value = addressee.domain?.let { "id_payment_requested_by_s|$it" }
                    _metadataImage.value = addressee.metadata.lnUrlPayImage()
                    _metadataDescription.value = addressee.metadata.lnUrlPayDescription()

                    _amountHint.value = if (addressee.isAmountLocked == false) {
                        ifNotNull(
                            addressee.minAmount,
                            addressee.maxAmount
                        ) { minAmount, maxAmount ->
                            "id_limits_s__s|${
                                minAmount.toAmountLook(
                                    session = session,
                                    withUnit = false
                                )
                            }|${
                                maxAmount.toAmountLook(
                                    session = session,
                                    withUnit = true
                                )
                            }"
                        }
                    } else null

                    if (addressee.bip21Params?.hasAmount == true || addressee.isGreedy == true || addressee.isAmountLocked == true) {
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
                    if((amount.value.isBlank() && !isSendAll.value) && listOf("id_invalid_amount", "id_amount_below_the_dust_threshold", "id_insufficient_funds", "id_amount_must_be_at_least_s", "id_amount_must_be_at_most_s").startsWith(it)){
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

        }, mutex = createTransactionMutex, preAction = {
            onProgress.value = true
            _isValid.value = false
        }, onSuccess = {
            _isValid.value = it != null
            if (it != null) {
                // Preserve error
                _error.value = null
            }

            if(finalCheckBeforeContinue && params != null && it != null){
                session.pendingTransaction = Triple(
                    first = params,
                    second = it,
                    third = TransactionSegmentation(
                        transactionType = TransactionType.SEND,
                        addressInputType = _addressInputType,
                        sendAll = isSendAll.value ?: false
                    )
                )
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SendConfirm(
                    accountAsset = accountAsset.value!!,
                    denomination = denomination.value
                )))
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

    private fun sendLightningTransaction(){
        doAsync({
            countly.startSendTransaction()
            countly.startFailedTransaction()

            createTransactionParams.value?.let {
                session.sendTransaction(
                    account = account,
                    signedTransaction = session.createTransaction(_network.value!!, it),
                    twoFactorResolver = this
                )
            }?: run {
                throw Exception("Something went wrong while creating the Transaction")
            }

        }, preAction = {
            onProgress.value = true
            _onProgressSending.value = true
            _navData.value = _navData.value.copy(
                isVisible = false
            )
        }, postAction = {
            (it == null).also {
                _onProgressSending.value = it
                onProgress.value = it
            }
            _navData.value = _navData.value.copy(
                isVisible = true
            )
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

            if(it.hasMessageOrUrl){
                postSideEffect(SideEffects.TransactionSent(it))
            }else{
                postSideEffect(SideEffects.NavigateToRoot)
            }
            postSideEffect(SideEffects.Snackbar("id_transaction_sent"))

        }, onError = {

            postSideEffect(
                SideEffects.ErrorDialog(
                    it, errorReport = (it as? ExceptionWithErrorReport)?.errorReport
                        ?: ErrorReport.create(
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

    private fun checkAccountAsset(
        accountAsset: AccountAsset,
        network: Network,
        assetId: String = network.policyAsset
    ): AccountAsset? = accountAsset.takeIf {
        it.account.network.isSameNetwork(network) && it.assetId == assetId && it.balance(session) > 0
    }

    private fun findAccountAsset(network: Network, assetId: String = network.policyAsset): AccountAsset? {
        val accountsAndAssets = session.accountAsset.value

        // Check current selected
        return (accountAsset.value?.let {
            checkAccountAsset(it, network = network, assetId = assetId)
        } ?:
        // Check current selected account, with assetId
        accountsAndAssets.find {
            it.account.id == accountAsset.value?.account?.id && it.assetId == assetId
        }?.let {
            checkAccountAsset(it, network = network, assetId = assetId)
        } ?:
        // Check active account
        sessionOrNull?.activeAccount?.value?.let {
            checkAccountAsset(it.accountAsset, network = network, assetId = assetId)
        } ?:
        // Check active account, with assetId
        accountsAndAssets.find {
            it.account.id == session.activeAccount.value?.id && it.assetId == assetId
        }?.let {
            checkAccountAsset(it, network = network, assetId = assetId)
        } ?:
        // Find first proper account
        accountsAndAssets.find {
            checkAccountAsset(it, network = network, assetId = assetId) != null
        }) ?:
        // Find first account of same network no matter the balance
        session.accounts.value.find {
            it.network.isSameNetwork(network)
        }?.let {
            AccountAsset.fromAccountAsset(it, assetId, session)
        }
    }

    companion object : Loggable() {
        val DustLimit = 546
    }
}

class SendViewModelPreview(greenWallet: GreenWallet) :
    SendViewModelAbstract(greenWallet = greenWallet) {
    override val isAccountEdit: StateFlow<Boolean> = MutableStateFlow(true)
    override val errorAddress: StateFlow<String?> = MutableStateFlow(null)
    override val errorAmount: StateFlow<String?> = MutableStateFlow(null)
    override val errorGeneric: StateFlow<String?> = MutableStateFlow(null)
    override val assetsAndAccounts: StateFlow<List<AccountAssetBalance>?> =
        MutableStateFlow(listOf())
//    override val accountAssetBalance: StateFlow<AccountAssetBalance?> =
//        MutableStateFlow(previewAccountAsset().let {
//            AccountAssetBalance(account = it.account, asset = it.asset)
//        })

    private val base64Png = "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"

    override val address: MutableStateFlow<String> = MutableStateFlow("address")
    override val amount: MutableStateFlow<String> = MutableStateFlow("0.1")
    override val amountExchange: StateFlow<String> = MutableStateFlow("0.1 USD")
    override val amountHint: StateFlow<String?> = MutableStateFlow(null)
    override val isAmountLocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val isSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val supportsSendAll: StateFlow<Boolean> = MutableStateFlow(true)
    override val metadataDomain: StateFlow<String?> = MutableStateFlow("id_payment_requested_by_s|blockstream.com")
    override val metadataImage: StateFlow<ByteArray?> = MutableStateFlow(base64Png.base64DecodedBytes)
    override val metadataDescription: StateFlow<String?> = MutableStateFlow("Metadata Description")


    init {
        _showFeeSelector.value = true
        banner.value = Banner.preview3
    }


    companion object {
        fun preview() = SendViewModelPreview(previewWallet())
    }
}
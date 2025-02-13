package com.blockstream.common.models.exchange

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_transfer
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.Banner
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
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
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.startsWith
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.PendingTransaction
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.gdk.params.toJsonElement
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.common.utils.getStringFromId
import com.blockstream.common.utils.ifNotNull
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import kotlin.math.absoluteValue

abstract class AccountExchangeViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null
) : CreateTransactionViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = accountAssetOrNull
) {
    override fun screenName(): String = "AccountExchange"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    val fromAccountAsset = accountAsset

    open val fromAccountAssetBalance: StateFlow<AccountAssetBalance?> = accountAssetBalance

    @NativeCoroutinesState
    val toAccountAsset: MutableStateFlow<AccountAsset?> = MutableStateFlow(null)

    @NativeCoroutinesState
    val toAccount: StateFlow<Account?> = toAccountAsset.map {
        it?.account
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    @NativeCoroutinesState
    abstract val errorAmount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val errorGeneric: StateFlow<String?>

    @NativeCoroutinesState
    abstract val toAccounts: StateFlow<List<AccountAssetBalance>?>

    @NativeCoroutinesState
    abstract val fromAccounts: StateFlow<List<AccountAssetBalance>?>

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amountExchange: StateFlow<String>

    @NativeCoroutinesState
    abstract val isSendAll: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val supportsSendAll: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val receiveAmount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val receiveAmountExchange: StateFlow<String?>
}

class AccountExchangeViewModel(
    greenWallet: GreenWallet,
    val initialAccountAssetOrNull: AccountAsset? = null,
) : AccountExchangeViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = initialAccountAssetOrNull
) {

    private val _supportsSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val supportsSendAll: StateFlow<Boolean> = _supportsSendAll.asStateFlow()

    override val isSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val assetId: MutableStateFlow<String?> = MutableStateFlow(null)

    override val fromAccounts = combine(session.accountAsset.map {
        // Only same network transfers
        it.filter { !it.account.isLightning }
    }, _denomination) { accounts, denomination ->
        accounts.mapNotNull {
            AccountAssetBalance.createIfBalance(
                accountAsset = it,
                session = sessionOrNull,
                denomination = denomination
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val toAccounts = combine(
        session.accounts,
        fromAccountAssetBalance,
        fromAccounts,
        _denomination
    ) { accounts, fromAccountBalance, _, denomination ->
        if (fromAccountBalance != null) {
            accounts.filter {
                fromAccountBalance.account.id != it.id // exclude same account
                        && (it.network.isSameNetwork(fromAccountBalance.account.network) || (fromAccountBalance.account.isBitcoin && it.isLightning)) // exclude different networks (at least for now)
                        && (!fromAccountBalance.asset.isAmp || it.isAmp) // if AMP asset, only AMP accounts
            }.map {
                AccountAssetBalance.create(
                    accountAsset = AccountAsset.fromAccountAsset(
                        account = it,
                        assetId = fromAccountBalance.accountAsset.assetId,
                        session = session
                    ), session = sessionOrNull, denomination = denomination
                )
            }
        } else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _errorAmount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorAmount: StateFlow<String?> = _errorAmount.asStateFlow()

    private val _errorGeneric: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorGeneric: StateFlow<String?> = _errorGeneric.asStateFlow()

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

    private val _receiveAmount: MutableStateFlow<String?> = MutableStateFlow(null)
    override val receiveAmount: StateFlow<String?> = _receiveAmount.asStateFlow()

    private val _receiveAmountExchange: MutableStateFlow<String?> = MutableStateFlow(null)
    override val receiveAmountExchange: StateFlow<String?> = _receiveAmountExchange.asStateFlow()

    private val _toAddress: StateFlow<String?> = toAccountAsset.map {
        it?.let {
            try {
                withContext(Dispatchers.IO) {
                    session.getReceiveAddressAsString(it.account)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var _pendingSetAccountFrom = true

    private val _isWatchOnly = MutableStateFlow(false)
    override val isWatchOnly = _isWatchOnly

    class LocalEvents {
        object ToggleIsSendAll : Event
        data class SetToAccount(val accountAsset: AccountAsset) : Event
        data class ClickAccount(val isFrom: Boolean = false) : Event
    }

    init {

        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_account_transfer), subtitle = greenWallet.name)
        }

        session.ifConnected {

            _isWatchOnly.value = session.isWatchOnly

            fromAccountAsset.onEach { fromAccount ->
                _network.value = fromAccount?.account?.network

                // Clear toAccount if network is not the same
                ifNotNull(fromAccount, toAccountAsset.value) { from, to ->
                    if (!from.account.network.isSameNetwork(to.account.network) || from.account.id == to.account.id) {
                        toAccountAsset.value = null
                    } else {
                        // Be sure to update the Asset
                        toAccountAsset.value = AccountAsset.fromAccountAsset(
                            account = to.account,
                            assetId = from.assetId,
                            session = session
                        )
                    }
                }
            }.launchIn(this)

            // When changing between accounts, reset isSendAll flag
            accountAsset.onEach {
                // Reset amount if it was prefilled from isSendAll
                if (isSendAll.value) {
                    amount.value = ""
                }
                isSendAll.value = false
                _supportsSendAll.value = it?.account?.isLightning == false
            }.launchIn(this)

            // When changing between different asset clear amount
            accountAsset.map {
                it?.assetId
            }.distinctUntilChanged().onEach {
                amount.value = ""
            }.launchIn(this)

            combine(fromAccountAsset, toAccount, _feeEstimation) { fromAccountAsset, toAccountAsset, _ ->
                val fromNetwork = fromAccountAsset?.account?.network

                _showFeeSelector.value =
                    fromAccountAsset != null
                            && toAccountAsset != null
                            && (fromNetwork?.isBitcoin == true || (fromNetwork?.isLiquid == true && getFeeRate(FeePriority.High()) > fromNetwork.defaultFee))

            }.launchIn(this)

            combine(
                session.accountAsset,
                fromAccountAsset,
                _toAddress,
                amount,
                isSendAll,
                _feePriorityPrimitive,
                denomination
            ) {
                val network = accountAsset.value?.account?.network

                // Check if the current AccountAsset operates on the same network only.
                // That way we preserve the asset from previous action
                if (network != null && (accountAsset.value?.let {
                        !it.account.network.isSameNetwork(
                            network
                        ) || it.balance(session) <= 0
                    } != false)) {
                    accountAsset.value =
                        findAccountAsset(network, assetId = assetId.value ?: network.policyAsset)
                }

                // Prefer the real network from the account
                _network.value = network?.let { accountAsset.value?.account?.network } ?: network
            }.onEach {
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
                _errorGeneric.value = it.takeIf { _errorAmount.value.isNullOrBlank() }?.let { getStringFromId(it) }
            }.launchIn(this)
        }

        bootstrap()
    }


    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClickAccount -> {
                _pendingSetAccountFrom = event.isFrom
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Accounts(
                            greenWallet = greenWallet,
                            accounts = AccountAssetBalanceList((if (event.isFrom) fromAccounts.value else toAccounts.value)
                                ?: listOf()),
                            withAsset = event.isFrom
                        )
                    )
                )
            }

            is LocalEvents.ToggleIsSendAll -> {
                isSendAll.value = isSendAll.value.let { isSendAll ->
                    if (isSendAll) {
                        amount.value = ""
                    }
                    !isSendAll
                }
            }

            is LocalEvents.SetToAccount -> {
                if (_pendingSetAccountFrom) {
                    fromAccountAsset.value = event.accountAsset
                } else {
                    toAccountAsset.value = event.accountAsset
                }
            }

            is Events.Continue -> {
                createTransactionParams.value?.also {
                    createTransaction(params = it, finalCheckBeforeContinue = true)
                }
            }
        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams? {
        val fromAccountAsset = fromAccountAsset.value
        val address = _toAddress.value

        if (fromAccountAsset == null || address == null) {
            _error.value = null
            return null
        }

        return (if (fromAccountAsset.account.network.isLightning) {
            null
            // No support for LN transactions yet
//            val satoshi = UserInput.parseUserInputSafe(
//                session = session,
//                input = amount.value,
//                denomination = denomination.value
//            ).getBalance(onlyInAcceptableRange = false)?.satoshi
//
//            AddressParams(
//                address = address,
//                satoshi = satoshi ?: 0
//            ).let { params ->
//                CreateTransactionParams(
//                    addressees = listOf(params.toJsonElement()),
//                    addresseesAsParams = listOf(params)
//                )
//            }
        } else {
            val isGreedy = isSendAll.value
            val satoshi = if (isGreedy) 0 else UserInput.parseUserInputSafe(
                session = session,
                input = amount.value,
                assetId = fromAccountAsset.assetId,
                denomination = denomination.value
            ).getBalance(onlyInAcceptableRange = false)?.satoshi

            val unspentOutputs = fromAccountAsset.account.let { session.getUnspentOutputs(it) }

            AddressParams(
                address = address,
                satoshi = satoshi ?: 0,
                isGreedy = isGreedy,
                assetId = fromAccountAsset.assetId.takeIf { fromAccountAsset.account.network.isLiquid }
            ).let { params ->
                CreateTransactionParams(
                    from = fromAccountAsset,
                    to = toAccountAsset.value,
                    addressees = listOf(params).toJsonElement(),
                    feeRate = getFeeRate(),
                    utxos = unspentOutputs.unspentOutputs
                )
            }
        })
    }

    override fun createTransaction(
        params: CreateTransactionParams?,
        finalCheckBeforeContinue: Boolean
    ) {
        doAsync({
            if (params == null) {
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

                    addressee.bip21Params?.assetId?.let { assetId ->
                        this.assetId.value = assetId
                        this.accountAsset.value = findAccountAsset(
                            network = network,
                            assetId = assetId
                        )
                    } ?: kotlin.run {
                        assetId.value = null
                    }

                    val assetId = addressee.assetId ?: account.network.policyAsset

                    if (addressee.isGreedy == true) {

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
                        }).also {
                            amount.value = it ?: ""
                        }
                    }

                    tx.satoshi[assetId]?.absoluteValue?.also { sendAmount ->
                        _receiveAmount.value = sendAmount.toAmountLook(
                            session = session,
                            assetId = assetId,
                            denomination = denomination.value,
                            withUnit = true,
                            withGrouping = false
                        )

                        _receiveAmountExchange.value = sendAmount.toAmountLook(
                            session = session,
                            assetId = assetId,
                            denomination = Denomination.exchange(session, denomination.value),
                            withUnit = true,
                            withGrouping = false,
                        )
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
            createTransaction.value = it
            _isValid.value = it != null

            if (it == null) {
                _receiveAmount.value = null
                _receiveAmountExchange.value = null
            } else {
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

                postSideEffect(SideEffects.Navigate())
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
            _receiveAmount.value = null
            _receiveAmountExchange.value = null
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

    private fun findAccountAsset(
        network: Network,
        assetId: String = network.policyAsset
    ): AccountAsset? {
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
        // Check initial account
        initialAccountAssetOrNull?.let {
            checkAccountAsset(it, network = network, assetId = assetId)
        } ?:
        // Check initial account, with assetId
        accountsAndAssets.find {
            it.account.id == initialAccountAssetOrNull?.account?.id && it.assetId == assetId
        }?.let {
            checkAccountAsset(it, network = network, assetId = assetId)
        } ?:
        // Find first proper account
        accountsAndAssets.find {
            checkAccountAsset(it, network = network, assetId = assetId) != null
        })
    }

    companion object : Loggable() {
        val DustLimit = 546
    }
}

class AccountExchangeViewModelPreview(greenWallet: GreenWallet) :
    AccountExchangeViewModelAbstract(greenWallet = greenWallet) {

    override val fromAccountAssetBalance: StateFlow<AccountAssetBalance?> = MutableStateFlow(
        previewAccountAssetBalance()
    )

    override val errorAmount: StateFlow<String?> = MutableStateFlow(null)
    override val errorGeneric: StateFlow<String?> = MutableStateFlow(null)
    override val fromAccounts: StateFlow<List<AccountAssetBalance>?> =
        MutableStateFlow(listOf())
    override val toAccounts: StateFlow<List<AccountAssetBalance>?> = MutableStateFlow(listOf())

    override val amount: MutableStateFlow<String> = MutableStateFlow("0.1")
    override val amountExchange: StateFlow<String> = MutableStateFlow("0.1 USD")

    override val receiveAmount: StateFlow<String?> = MutableStateFlow(null)
    override val receiveAmountExchange: StateFlow<String?> = MutableStateFlow(null)

    override val isSendAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val supportsSendAll: StateFlow<Boolean> = MutableStateFlow(true)

    override val isWatchOnly: StateFlow<Boolean> = MutableStateFlow(false)

    init {

        previewAccountAssetBalance().also {
            _network.value = it.account.network
            fromAccountAsset.value = it.accountAsset
        }

        toAccountAsset.value = previewAccountAsset()

        _showFeeSelector.value = true
        banner.value = Banner.preview3
    }


    companion object {
        fun preview() = AccountExchangeViewModelPreview(previewWallet())
    }
}
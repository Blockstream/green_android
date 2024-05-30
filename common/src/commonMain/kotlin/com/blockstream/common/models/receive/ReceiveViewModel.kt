package com.blockstream.common.models.receive

import breez_sdk.InputType
import breez_sdk.LnInvoice
import com.blockstream.common.AddressType
import com.blockstream.common.MediaType
import com.blockstream.common.Urls
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.lightning.expireIn
import com.blockstream.common.lightning.feeSatoshi
import com.blockstream.common.lightning.inboundLiquiditySatoshi
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.receiveAmountSatoshi
import com.blockstream.common.lightning.satoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toAmountLookOrNa
import com.eygraber.uri.Uri
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class ReceiveViewModelAbstract(greenWallet: GreenWallet, accountAssetOrNull: AccountAsset?) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    override fun screenName(): String = "Receive"

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amountError: StateFlow<String?>

    @NativeCoroutinesState
    abstract val requestAmount: StateFlow<String?>

    @NativeCoroutinesState
    abstract val note: StateFlow<String?>

    @NativeCoroutinesState
    abstract val liquidityFee: StateFlow<String?>

    @NativeCoroutinesState
    abstract val onchainSwapMessage: StateFlow<String?>

    @NativeCoroutinesState
    abstract val amountCurrency: StateFlow<String>

    @NativeCoroutinesState
    abstract val amountExchange: StateFlow<String>

    @NativeCoroutinesState
    abstract val maxReceiveAmount: StateFlow<String>

    @NativeCoroutinesState
    abstract val invoiceAmountToReceive: StateFlow<String?>

    @NativeCoroutinesState
    abstract val invoiceAmountToReceiveFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val invoiceDescription: StateFlow<String?>

    @NativeCoroutinesState
    abstract val invoiceExpiration: StateFlow<String?>

    @NativeCoroutinesState
    abstract val invoiceExpirationTimestamp: StateFlow<Long?>

    @NativeCoroutinesState
    abstract val receiveAddress: StateFlow<String?>

    @NativeCoroutinesState
    abstract val showVerifyOnDevice: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showLightningOnChainAddress: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showRequestAmountEdit: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showLedgerAssetWarning: StateFlow<Boolean>

}

class ReceiveViewModel(initialAccountAsset: AccountAsset, greenWallet: GreenWallet) :
    ReceiveViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = initialAccountAsset) {

    private val _receiveAddress = MutableStateFlow<String?>(null)
    private val _showVerifyOnDevice = MutableStateFlow(false)
    private val _showLightningOnChainAddress = MutableStateFlow(false)
    private val _showLedgerAssetWarning = MutableStateFlow(false)
    private val _showRequestAmountEdit = MutableStateFlow(false)

    private val _requestAmount = MutableStateFlow<String?>(null)
    private val _amountError = MutableStateFlow<String?>(null)
    private val _note = MutableStateFlow<String?>(null)
    private val _liquidityFee = MutableStateFlow<String?>(null)
    private val _onchainSwapMessage = MutableStateFlow<String?>(null)
    private val _amountCurrency = MutableStateFlow("")
    private val _amountExchange = MutableStateFlow("")
    private val _maxReceiveAmount = MutableStateFlow("")

    private val _invoiceAmountToReceive = MutableStateFlow<String?>(null)
    private val _invoiceAmountToReceiveFiat = MutableStateFlow<String?>(null)
    private val _invoiceDescription = MutableStateFlow<String?>(null)
    private val _invoiceExpiration = MutableStateFlow<String?>(null)
    private val _invoiceExpirationTimestamp = MutableStateFlow<Long?>(null)

    override val receiveAddress: StateFlow<String?> = _receiveAddress.asStateFlow()

    override val invoiceAmountToReceive = _invoiceAmountToReceive.asStateFlow()
    override val invoiceAmountToReceiveFiat = _invoiceAmountToReceiveFiat.asStateFlow()
    override val invoiceDescription = _invoiceDescription.asStateFlow()
    override val invoiceExpiration = _invoiceExpiration.asStateFlow()
    override val invoiceExpirationTimestamp = _invoiceExpirationTimestamp.asStateFlow()

    override val amount = MutableStateFlow("")
    override val amountError = _amountError.asStateFlow()
    override val requestAmount = _requestAmount.asStateFlow()
    override val note = _note.asStateFlow()
    override val liquidityFee = _liquidityFee.asStateFlow()
    override val onchainSwapMessage = _onchainSwapMessage.asStateFlow()
    override val amountCurrency = _amountCurrency.asStateFlow()
    override val amountExchange = _amountExchange.asStateFlow()
    override val maxReceiveAmount = _maxReceiveAmount.asStateFlow()
    override val showVerifyOnDevice = _showVerifyOnDevice.asStateFlow()
    override val showLightningOnChainAddress = _showLightningOnChainAddress.asStateFlow()
    override val showLedgerAssetWarning = _showLedgerAssetWarning.asStateFlow()
    override val showRequestAmountEdit = _showRequestAmountEdit.asStateFlow()


    private val _address = MutableStateFlow<Address?>(null)
    private val _lightningInvoice = MutableStateFlow<LnInvoice?>(null)

    class LocalEvents {
        object ToggleLightning: Event
        object GenerateNewAddress: Event
        object CreateInvoice: Event
        object CopyAddress: Event
        object ShareAddress: Event
        object ShareQR: Event
        object VerifyOnDevice: Event
        object ClearLightningInvoice: Event
        class SetNote(val note: String): Event
        class SetRequestAmount(val amount: String?): Event
        object ClickFundingFeesLearnMore : Events.OpenBrowser(Urls.HELP_FUNDING_FEES)
    }

    class LocalSideEffects {
        class VerifyOnDevice(val address: String): SideEffect
        class VerifiedOnDevice(val verified: Boolean) : SideEffect
        class ShareAddress(val address: String) : SideEffect
        class ShareQR(val address: String) : SideEffect
    }

    private val _generateAddressLock = Mutex()

    init {
        _navData.value = NavData(title = "id_receive", subtitle = greenWallet.name)

        session.ifConnected {
            session.activeAccount.filterNotNull().drop(1).onEach {
                 // accountAsset.value = it.accountAsset
            }.launchIn(this)

            accountAsset.onEach {
                _requestAmount.value = null
                _showLightningOnChainAddress.value = false
            }.launchIn(this)

            combine(accountAsset, _requestAmount) { _, _, ->

            }.onEach {
                generateAddress()
            }.launchIn(this)


            // lightningSdk is not null
            session.lightning?.takeIf { session.hasLightning }?.also {
                // Support single lightning account, else we have to incorporate account change events
                val lightningAccount = session.lightningAccount

                combine(session.lightningSdk.lspInfoStateFlow, amount, denomination) { _, _, _ ->

                }.onEach { _ ->
                    updateAmountExchangeRate()
                }.launchIn(viewModelScope.coroutineScope)

                denomination
                    .onEach {
                        _amountCurrency.value =
                            it.unit(session, lightningAccount.network.policyAsset)
                    }.launchIn(viewModelScope.coroutineScope)

                combine(session.lightningSdkOrNull?.nodeInfoStateFlow ?: emptyFlow() , denomination) { nodeState, _ ->
                    nodeState
                }.onEach {
                    _maxReceiveAmount.value = it.maxReceivableSatoshi().toAmountLook(
                        session = session,
                        assetId = lightningAccount.network.policyAsset,
                        denomination = denomination.value,
                        withUnit = true
                    )?.let {
                        "id_max_limit_s|$it"
                    } ?: ""

                    updateAmountExchangeRate()
                }.launchIn(viewModelScope.coroutineScope)

                session.lastInvoicePaid.filterNotNull().onEach { paidDetails ->
                    if (paidDetails.paymentHash == _lightningInvoice.value?.paymentHash) {
                        (withContext(context = Dispatchers.IO) {
                            // Parse the actual Bolt11 invoice
                            session.parseInput(paidDetails.bolt11)
                        }?.second as? InputType.Bolt11)?.also {
                            postSideEffect(SideEffects.Success(it.invoice))
                            _lightningInvoice.value = null
                        }
                    }
                }.launchIn(viewModelScope.coroutineScope)
            }
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClearLightningInvoice -> {
                _receiveAddress.value = null
            }
            is LocalEvents.VerifyOnDevice -> {
                if(!onProgress.value) {
                    _address.value?.also {
                        verifyAddressOnDevice()
                        postSideEffect(LocalSideEffects.VerifyOnDevice(it.address))
                    }
                }
            }
            is LocalEvents.CopyAddress -> {
                postSideEffect(
                    SideEffects.CopyToClipboard(
                        value = receiveAddress.value ?: "",
                        message = "id_address_copied_to_clipboard",
                        label = "Address"
                    )
                )

                countly.receiveAddress(
                    addressType = if(_requestAmount.value.isNullOrBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.TEXT,
                    account = account,
                    session = session
                )
            }

            is LocalEvents.ShareAddress -> {
                countly.receiveAddress(
                    addressType = if(_requestAmount.value.isNullOrBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.TEXT,
                    isShare = true,
                    account = account,
                    session = session
                )
                postSideEffect(LocalSideEffects.ShareAddress(receiveAddress.value ?: ""))
            }

            is LocalEvents.ShareQR -> {
                countly.receiveAddress(
                    addressType = if(_requestAmount.value.isNullOrBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.IMAGE,
                    isShare = true,
                    account = account,
                    session = session
                )
                postSideEffect(LocalSideEffects.ShareQR(receiveAddress.value ?: ""))
            }
            is LocalEvents.ToggleLightning -> {
                if(_showLightningOnChainAddress.value){
                    _showLightningOnChainAddress.value = false
                } else {
                    createOnchainSwap()
                }
            }
            is LocalEvents.GenerateNewAddress -> {
                generateAddress()
            }

            is LocalEvents.CreateInvoice -> {
                createLightningInvoice()
            }

            is LocalEvents.SetNote -> {
                _note.value = event.note
            }

            is LocalEvents.SetRequestAmount -> {
                _requestAmount.value = event.amount
            }

        }
    }

    override fun errorReport(exception: Throwable): ErrorReport {
        return ErrorReport.create(throwable = exception, network = account.network, session = session)
    }

    private fun verifyAddressOnDevice() {
        countly.verifyAddress(session, account)

        _address.value?.let { address ->
            session.gdkHwWallet?.let { hwWallet ->
                doAsync({
                    if(hwWallet.getGreenAddress(
                        network = account.network,
                        hwInteraction = null,
                        account = account,
                        path = address.userPath ?: listOf(),
                        csvBlocks = address.subType ?: 0
                    ) != address.address){
                        throw Exception("id_the_addresses_dont_match")
                    }
                }, preAction = null, postAction = null, timeout = 30, onSuccess = {
                    postSideEffect(SideEffects.Snackbar("id_the_address_is_valid"))
                    postSideEffect(LocalSideEffects.VerifiedOnDevice(true))
                }, onError = {
                    postSideEffect(SideEffects.ErrorDialog(it))
                    postSideEffect(LocalSideEffects.VerifiedOnDevice(false))
                })
            }
        }
    }

    private fun generateAddress() {
        logger.i { "Generating address for ${account.name}" }
        val network = account.network
        _showLedgerAssetWarning.value = account.isLiquid && session.device?.isLedger == true
        _showVerifyOnDevice.value = session.device?.let { device ->
            !account.isLightning && (
                    device.isJade ||
                            (device.isLedger && network.isLiquid && !network.isSinglesig) ||
                            (device.isLedger && !network.isLiquid && network.isSinglesig) ||
                            (device.isTrezor && !network.isLiquid && network.isSinglesig)
                    )
        } ?: false

        if (account.isLightning) {
            _receiveAddress.value = null
        } else {
            doAsync({
                _generateAddressLock.withLock {
                    session.getReceiveAddress(account)
                }
            }, onSuccess = {
                _address.value = it
                updateAddress(it.address)
            })
        }
    }

    private fun updateAddress(address: String) {

        if (account.isLightning) {
            if (showLightningOnChainAddress.value) {
                _receiveAddress.value = address.takeIf { it.isNotBlank() }
                // isAddressUri.value = false
            } else {
                _receiveAddress.value = address.takeIf { it.isNotBlank() }?.let {
                    // isAddressUri.value = false
                    Uri.Builder().also {
                        it.scheme(account.network.bip21Prefix)
                        it.opaquePart(address.uppercase()) // bech32 is case insensitive
                    }.toString()
                }
            }
        } else if (_requestAmount.value != null) {
            // isAddressUri.value = true

            // Use 2 different builders, we are restricted by spec
            // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part
            val scheme = Uri.Builder().also {
                it.scheme(account.network.bip21Prefix)
                it.opaquePart(address)
            }.toString()

            val query = Uri.Builder().also {
                if (!_requestAmount.value.isNullOrBlank()) {
                    it.appendQueryParameter("amount", _requestAmount.value)
                }

                if (account.network.isLiquid) {
                    it.appendQueryParameter("assetid", accountAsset.value!!.asset.assetId)
                }
            }.toString()

            _receiveAddress.value = scheme + query
        } else {
            // isAddressUri.value = false
            _receiveAddress.value = address
        }
    }

    private fun createLightningInvoice(){
        doAsync({
            val amount = UserInput.parseUserInput(
                session = session,
                input = amount.value,
                assetId = account.network.policyAsset,
                denomination = denomination.value
            ).getBalance()?.satoshi ?: 0

            val response = session.createLightningInvoice(amount, note.value ?: "")

            val receiveAmount = response.receiveAmountSatoshi()

            _invoiceAmountToReceive.value = receiveAmount.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                denomination = denomination.value.takeIf { !it.isFiat } ?: Denomination.default(session),
                withUnit = true
            )

            _invoiceAmountToReceiveFiat.value = receiveAmount.toAmountLookOrNa(
                session = session,
                assetId = account.network.policyAsset,
                denomination = Denomination.fiat(session),
                withUnit = true
            )

            _invoiceExpirationTimestamp.value = response.lnInvoice.expireIn().toEpochMilliseconds()
                // TODO make it KMP
//            _invoiceExpiration.value = DateUtils.getRelativeTimeSpanString(
//                invoice.expireIn().toEpochMilliseconds(),
//                System.currentTimeMillis(),
//                DateUtils.SECOND_IN_MILLIS
//            ).toString()

            _lightningInvoice.value  = response.lnInvoice

            response
        }, onSuccess = {
            updateAddress(it.lnInvoice.bolt11)
        })
    }

    private fun createOnchainSwap(){
        doAsync({
            val swapInfo = session.receiveOnchain()
            swapInfo to (swapInfo.channelOpeningFees?.minMsat?.satoshi()?.toAmountLook(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true
            ) ?: "-")

            _onchainSwapMessage.value = "id_send_more_than_s_and_up_to_s_to_this|${
                swapInfo.minAllowedDeposit.toAmountLookOrNa(
                    session = session,
                    assetId = session.lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true,
                )
            }|${
                swapInfo.maxAllowedDeposit.toAmountLookOrNa(
                    session = session,
                    assetId = session.lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                )
            }|${
                swapInfo.channelOpeningFees?.minMsat?.satoshi()?.toAmountLook(
                    session = session,
                    assetId = account.network.policyAsset,
                    withUnit = true
                ) ?: "-"
            }"

            _receiveAddress.value = swapInfo.bitcoinAddress

            swapInfo
        }, onSuccess = {
            _showLightningOnChainAddress.value = true
            updateAddress(it.bitcoinAddress)
        })
    }

    private fun updateAmountExchangeRate() {
        // Convert between BTC / Fiat
        doAsync({
            val nodeState = session.lightningSdk.nodeInfoStateFlow.value

            val balance = amount.value.takeIf { it.isNotBlank() }?.let {
                UserInput.parseUserInput(
                    session = session,
                    input = it,
                    assetId = accountAsset.value!!.asset.assetId,
                    denomination = denomination.value
                ).getBalance()
            }

            (balance?.let {
                "≈ " + it.toAmountLook(
                    session = session,
                    assetId = accountAsset.value!!.asset.assetId,
                    denomination = Denomination.exchange(
                        session = session,
                        denomination = denomination.value
                    ),
                    withUnit = true,
                    withGrouping = true,
                    withMinimumDigits = false
                )
            } ?: "").also {
                _amountExchange.value = it
            }

            val openChannelFee = balance?.satoshi?.let {
                if (it > nodeState.inboundLiquiditySatoshi()) session.lightningSdk.openChannelFee(
                    it
                ) else null
            }

            _isValid.value = balance != null && (balance.satoshi >= 0 &&
                    balance.satoshi <= nodeState.maxReceivableSatoshi() &&
                    (balance.satoshi <= nodeState.inboundLiquiditySatoshi() || (balance.satoshi > (openChannelFee?.feeSatoshi()
                        ?: 0)))
                    )

            _amountError.value = if (amount.value.isBlank()) null else {
                if (balance != null) {
                    val maxReceivableSatoshi = nodeState.maxReceivableSatoshi()
                    val channelMinimum = openChannelFee?.feeSatoshi() ?: 0
                    if (balance.satoshi > maxReceivableSatoshi) {
                        "id_you_cannot_receive_more_than_s|${
                            maxReceivableSatoshi.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = denomination.value.notFiat()
                            )
                        }|${
                            maxReceivableSatoshi.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = Denomination.fiat(session)
                            )
                        }"
                    } else if (balance.satoshi <= channelMinimum) {
                        "id_this_amount_is_below_the|${
                            channelMinimum.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = denomination.value.notFiat()
                            )
                        }|${
                            channelMinimum.toAmountLook(
                                session = session,
                                withUnit = true,
                                denomination = Denomination.fiat(session)
                            )
                        }"
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            val isSetupChannel = nodeState.inboundLiquiditySatoshi() == 0L

            val channelFee = openChannelFee?.feeSatoshi()?.toAmountLook(
                session = session,
                assetId = account.network.policyAsset,
                denomination = denomination.value.notFiat(),
                withUnit = true
            ) ?: "-"

            val channelFeeFiat = openChannelFee?.feeSatoshi()?.toAmountLook(
                session = session,
                assetId = account.network.policyAsset,
                denomination = Denomination.fiat(session),
                withUnit = true
            ) ?: "-"

            _liquidityFee.value = when {
                amount.value.isBlank() -> {
                    null
                }
                isSetupChannel -> {
                    "id_a_set_up_funding_fee_of_s|${channelFee}|${channelFeeFiat}"
                }
                (balance?.satoshi ?: 0) > nodeState.inboundLiquiditySatoshi() -> {

                    val inboundLiquidity = nodeState.inboundLiquiditySatoshi().toAmountLookOrNa(
                        session = session,
                        assetId = session.lightningAccount.network.policyAsset,
                        denomination = denomination.value.notFiat(),
                        withUnit = true
                    )

                    val inboundLiquidityFiat = nodeState.inboundLiquiditySatoshi().toAmountLook(
                        session = session,
                        assetId = session.lightningAccount.network.policyAsset,
                        denomination = Denomination.fiat(session),
                        withUnit = true
                    ) ?: ""

                    "id_a_funding_fee_of_s_s|${channelFee}|${channelFeeFiat}|${inboundLiquidity}|${inboundLiquidityFiat}"
                }
                else -> null
            }

        }, preAction = null, postAction = null, onSuccess = { }, onError = {
            _amountExchange.value = ""
            _isValid.value = false
            _amountError.value = null
            _liquidityFee.value = null
        })
    }

    override suspend fun denominatedValue(): DenominatedValue {
        return UserInput.parseUserInputSafe(
            session,
            amount.value,
            denomination = denomination.value
        ).getBalance().let {
            DenominatedValue(
                balance = it,
                assetId = accountAsset.value?.assetId,
                denomination = denomination.value
            )
        }
    }

    override fun setDenominatedValue(denominatedValue: DenominatedValue) {
        amount.value = denominatedValue.asInput(session) ?: ""
        _denomination.value = denominatedValue.denomination
    }

    companion object : Loggable()

}

class ReceiveViewModelPreview() : ReceiveViewModelAbstract(greenWallet = previewWallet(), accountAssetOrNull = previewAccountAsset()) {

    override val receiveAddress: StateFlow<String?> = MutableStateFlow("bc1")
    override val amount: MutableStateFlow<String> = MutableStateFlow("")
    override val amountError: StateFlow<String?> = MutableStateFlow(null)
    override val requestAmount: StateFlow<String?> = MutableStateFlow(null)
    override val note = MutableStateFlow("")
    override val liquidityFee: StateFlow<String?> = MutableStateFlow(null)
    override val onchainSwapMessage: StateFlow<String?> = MutableStateFlow(null)
    override val amountCurrency: StateFlow<String> = MutableStateFlow("")
    override val amountExchange: StateFlow<String> = MutableStateFlow("")
    override val maxReceiveAmount: StateFlow<String> = MutableStateFlow("")
    override val invoiceAmountToReceive: MutableStateFlow<String?> = MutableStateFlow(null)
    override val invoiceAmountToReceiveFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val invoiceDescription: MutableStateFlow<String?> = MutableStateFlow(null)
    override val invoiceExpiration: MutableStateFlow<String?> = MutableStateFlow(null)
    override val invoiceExpirationTimestamp: StateFlow<Long?> = MutableStateFlow(null)
    override val showVerifyOnDevice: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLightningOnChainAddress: StateFlow<Boolean> = MutableStateFlow(false)
    override val showRequestAmountEdit: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLedgerAssetWarning: StateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = ReceiveViewModelPreview()
    }
}
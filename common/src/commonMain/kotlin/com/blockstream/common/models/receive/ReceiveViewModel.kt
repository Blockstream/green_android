package com.blockstream.common.models.receive

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.at
import blockstream_green.common.generated.resources.id_a_funding_fee_of_s_s_is_applied
import blockstream_green.common.generated.resources.id_a_set_up_funding_fee_of_s_s
import blockstream_green.common.generated.resources.id_address_copied_to_clipboard
import blockstream_green.common.generated.resources.id_funds_received
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.id_list_of_addresses
import blockstream_green.common.generated.resources.id_max_limit_s
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_please_hold_on_while_your
import blockstream_green.common.generated.resources.id_please_wait_until_your_ledger
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_request_amount
import blockstream_green.common.generated.resources.id_send_more_than_s_and_up_to_s_to
import blockstream_green.common.generated.resources.id_sweep_from_paper_wallet
import blockstream_green.common.generated.resources.id_the_address_is_valid
import blockstream_green.common.generated.resources.id_this_amount_is_below_the
import blockstream_green.common.generated.resources.id_you_cannot_receive_more_than_s
import blockstream_green.common.generated.resources.id_you_have_just_received_s
import blockstream_green.common.generated.resources.lightning_fill
import blockstream_green.common.generated.resources.note_pencil
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.question
import blockstream_green.common.generated.resources.text_aa
import breez_sdk.InputType
import breez_sdk.LnInvoice
import com.blockstream.common.AddressType
import com.blockstream.common.MediaType
import com.blockstream.common.Urls
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.lightning.amountSatoshi
import com.blockstream.common.lightning.expireIn
import com.blockstream.common.lightning.feeSatoshi
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.receiveAmountSatoshi
import com.blockstream.common.lightning.satoshi
import com.blockstream.common.lightning.totalInboundLiquiditySatoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.platformFileSystem
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.formatAuto
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toAmountLookOrNa
import com.eygraber.uri.Uri
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class ReceiveViewModelAbstract(greenWallet: GreenWallet, accountAssetOrNull: AccountAsset?) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    override fun screenName(): String = "Receive"

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amountError: StateFlow<String?>

    @NativeCoroutinesState
    abstract val showAmount: StateFlow<Boolean>

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
    abstract val receiveAddressUri: StateFlow<String?>

    @NativeCoroutinesState
    abstract val showVerifyOnDevice: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showLightningOnChainAddress: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showLedgerAssetWarning: StateFlow<Boolean>

}

class ReceiveViewModel(initialAccountAsset: AccountAsset, greenWallet: GreenWallet) :
    ReceiveViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = initialAccountAsset) {

    private val _receiveAddress = MutableStateFlow<String?>(null)
    private val _receiveAddressUri = MutableStateFlow<String?>(null)
    private val _showVerifyOnDevice = MutableStateFlow(false)
    private val _showLightningOnChainAddress = MutableStateFlow(false)
    private val _showLedgerAssetWarning = MutableStateFlow(false)
    private val _showRequestAmount = MutableStateFlow(false)

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

    override val receiveAddress: StateFlow<String?> = _receiveAddress
    override val receiveAddressUri: StateFlow<String?> = _receiveAddressUri

    override val invoiceAmountToReceive = _invoiceAmountToReceive
    override val invoiceAmountToReceiveFiat = _invoiceAmountToReceiveFiat
    override val invoiceDescription = _invoiceDescription
    override val invoiceExpiration = _invoiceExpiration
    override val invoiceExpirationTimestamp = _invoiceExpirationTimestamp

    override val amount = MutableStateFlow("")
    override val amountError = _amountError
    override val note = _note
    override val liquidityFee = _liquidityFee
    override val onchainSwapMessage = _onchainSwapMessage
    override val amountCurrency = _amountCurrency
    override val amountExchange = _amountExchange
    override val maxReceiveAmount = _maxReceiveAmount
    override val showVerifyOnDevice = _showVerifyOnDevice
    override val showLightningOnChainAddress = _showLightningOnChainAddress
    override val showLedgerAssetWarning = _showLedgerAssetWarning
    override val showAmount = _showRequestAmount

    private val _address = MutableStateFlow<Address?>(null)
    private val _lightningInvoice = MutableStateFlow<LnInvoice?>(null)

    private val _appConfig: AppConfig by inject()

    class LocalEvents {
        object ToggleLightning: Event
        object GenerateNewAddress: Event
        object CreateInvoice: Event
        object CopyAddress: Event
        object ShareAddress: Event
        class ShareQR(val data: ByteArray? = null): Event
        object VerifyOnDevice: Event
        object ClearLightningInvoice: Event
        class SetNote(val note: String): Event
        class SetRequestAmount(val amount: String?): Event
        object ClickFundingFeesLearnMore : Events.OpenBrowser(Urls.HELP_FUNDING_FEES)
        object ClickLedgerSupportedAssets : Events.OpenBrowser(Urls.LEDGER_SUPPORTED_ASSETS)
        object ShowRequestAmount: Event
    }

    private val _generateAddressLock = Mutex()

    init {
        combine(accountAsset, showLightningOnChainAddress, receiveAddress) { accountAsset, showLightningOnChainAddress, receiveAddress ->
            _navData.value = NavData(
                title = getString(Res.string.id_receive),
                subtitle = greenWallet.name,
                actions = listOfNotNull(
                    NavAction(
                        title = getString(Res.string.id_note),
                        icon = Res.drawable.note_pencil,
                        isMenuEntry = false,
                        onClick = {
                            postSideEffect(
                                SideEffects.NavigateTo(
                                    NavigateDestinations.Note(
                                        note = note.value ?: "",
                                        noteType = if(accountAsset?.account?.isLightning == true) NoteType.Description else NoteType.Note
                                    )
                                )
                            )
                        }
                    ).takeIf { accountAsset?.account?.isLightning == true && !showLightningOnChainAddress && receiveAddress == null},
                    NavAction(
                        title = getString(Res.string.id_help),
                        icon = Res.drawable.question,
                        isMenuEntry = false,
                        onClick = {
                            postSideEffect(SideEffects.OpenBrowser(if (accountAsset?.account?.isAmp == true) Urls.HELP_AMP_ASSETS else Urls.HELP_RECEIVE_ASSETS))
                        }
                    ),

                    NavAction(
                        title = getString(Res.string.id_request_amount),
                        icon = Res.drawable.text_aa,
                        isMenuEntry = true,
                        onClick = {
                            postEvent(ReceiveViewModel.LocalEvents.ShowRequestAmount)
                        }
                    ).takeIf { receiveAddress.isNotBlank() && accountAsset?.account?.isLightning == false },
                    NavAction(
                        title = getString(Res.string.id_list_of_addresses),
                        icon = Res.drawable.at,
                        isMenuEntry = true,
                        onClick = {
                            accountAsset?.also {
                                postEvent(NavigateDestinations.Addresses(accountAsset = it))
                            }
                        }
                    ).takeIf { receiveAddress.isNotBlank() && accountAsset?.account?.isLightning == false },
                    NavAction(
                        title = getString(Res.string.id_sweep_from_paper_wallet),
                        icon = Res.drawable.qr_code,
                        isMenuEntry = true,
                        onClick = {
                            accountAsset?.also {
                                postEvent(NavigateDestinations.Sweep(accountAsset = it))
                            }
                        }
                    ).takeIf { receiveAddress.isNotBlank() && accountAsset?.account?.isLightning == false && !accountAsset.account.isLiquid },
                ),
                onBackPressed = {
                    if(onProgress.value) {
                        showWaitSnackbar()
                    }
                    !onProgress.value
                },
            )

        }.launchIn(this)

        session.ifConnected {

            combine(accountAsset, showLightningOnChainAddress) { accountAsset, showLightningOnChainAddress ->
                // When toggling between showLightningOnChainAddress clear the receiveAddress
                if(accountAsset?.account?.isLightning == true && !showLightningOnChainAddress){
                    _receiveAddress.value = null
                    _receiveAddressUri.value = null
                    _address.value = null
                }

                amount.value = ""
                _amountError.value = null
                _note.value = null
                _showRequestAmount.value = false

                _invoiceAmountToReceive.value = null
                _invoiceDescription.value = null
                _invoiceExpiration.value = null
            }.launchIn(this)

            accountAsset.onEach {
                _showLightningOnChainAddress.value = false
                _liquidityFee.value = null
                generateAddress()
            }.launchIn(this)

            amount.onEach {
                _address.value?.also {
                    updateAddress(it.address)
                }
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
                        getString(Res.string.id_max_limit_s,it)
                    } ?: ""

                    updateAmountExchangeRate()
                }.launchIn(viewModelScope.coroutineScope)

                session.lastInvoicePaid.filterNotNull().onEach { paidDetails ->
                    if (paidDetails.paymentHash == _lightningInvoice.value?.paymentHash) {
                        // Parse the actual Bolt11 invoice
                        (session.parseInput(paidDetails.bolt11)?.second as? InputType.Bolt11)?.also {
                            postSideEffect(
                                SideEffects.Dialog(
                                    title = StringHolder.create(Res.string.id_funds_received),
                                    message = StringHolder(string =
                                        getString(
                                            Res.string.id_you_have_just_received_s,
                                            it.invoice.amountSatoshi()?.toAmountLook(
                                                session = session,
                                                withUnit = true,
                                                withGrouping = true
                                            ) ?: ""
                                        )
                                    ),
                                    icon = Res.drawable.lightning_fill
                                )
                            )
                            _lightningInvoice.value = null
                        }
                    }
                }.launchIn(viewModelScope.coroutineScope)
            }
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClearLightningInvoice -> {
                _receiveAddress.value = null
                _receiveAddressUri.value = null
                _invoiceAmountToReceive.value = null
                _invoiceAmountToReceiveFiat.value = null
                _invoiceDescription.value = null
                _invoiceExpiration.value = null
                _invoiceExpirationTimestamp.value = null
            }
            is LocalEvents.VerifyOnDevice -> {
                if(!onProgress.value) {
                    _address.value?.also {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.DeviceInteraction(
                                    verifyAddress = it.address
                                )
                            )
                        )
                        verifyAddressOnDevice()
                    }
                }
            }
            is LocalEvents.CopyAddress -> {
                postSideEffect(
                    SideEffects.CopyToClipboard(
                        value = receiveAddress.value ?: "",
                        message = getString(Res.string.id_address_copied_to_clipboard),
                        label = "Address"
                    )
                )

                countly.receiveAddress(
                    addressType = if(amount.value.isBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.TEXT,
                    account = account,
                    session = session
                )
            }

            is LocalEvents.ShareAddress -> {
                countly.receiveAddress(
                    addressType = if(amount.value.isBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.TEXT,
                    isShare = true,
                    account = account,
                    session = session
                )
                postSideEffect(SideEffects.Share(receiveAddress.value ?: ""))
            }

            is LocalEvents.ShareQR -> {
                countly.receiveAddress(
                    addressType = if(amount.value.isBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.IMAGE,
                    isShare = true,
                    account = account,
                    session = session
                )

                createQRImageAndShare(receiveAddress.value ?: "", event.data)
            }
            is LocalEvents.ToggleLightning -> {
                if(_showLightningOnChainAddress.value){
                    _showLightningOnChainAddress.value = false
                } else {
                    createOnchainSwap()
                }
            }
            is LocalEvents.GenerateNewAddress -> {
                if (onProgress.value) {
                    showWaitSnackbar()
                }else{
                    generateAddress()
                }
            }

            is LocalEvents.CreateInvoice -> {
                createLightningInvoice()
            }

            is LocalEvents.SetNote -> {
                _note.value = event.note
            }

            is LocalEvents.SetRequestAmount -> {
                amount.value = event.amount ?: ""
            }

            is LocalEvents.ShowRequestAmount -> {
                _showRequestAmount.value = true
            }
        }
    }

    private fun showWaitSnackbar() {
        postSideEffect(
            SideEffects.Snackbar(
                text = StringHolder.create(if (account.network.isLiquid && session.device?.isLedger == true) Res.string.id_please_wait_until_your_ledger else Res.string.id_please_hold_on_while_your)
            )
        )
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
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_the_address_is_valid)))
                    // Dismiss Verify Transaction Dialog
                    postSideEffect(SideEffects.Dismiss)
                }, onError = {
                    postSideEffect(SideEffects.ErrorDialog(it))
                    // Dismiss Verify Transaction Dialog
                    postSideEffect(SideEffects.Dismiss)
                })
            }
        }
    }

    private fun generateAddress() {
        logger.i { "Generating address for ${account.name}" }
        _showLedgerAssetWarning.value = account.isLiquid && session.device?.isLedger == true
        _showVerifyOnDevice.value = session.device?.canVerifyAddressOnDevice(account) ?: false

        if (account.isLightning) {
            _receiveAddress.value = null
            _receiveAddressUri.value = null
            _address.value = null
        } else {
            doAsync({
                session.getReceiveAddress(account)
            }, mutex = _generateAddressLock, onSuccess = {
                _address.value = it
                updateAddress(it.address)
            })
        }
    }

    private suspend fun updateAddress(address: String) {
        withContext(context = Dispatchers.IO) {

            if (account.isLightning && !showLightningOnChainAddress.value) {
                Uri.Builder().also {
                    it.scheme(account.network.bip21Prefix)
                    it.opaquePart(address.uppercase()) // bech32 is case insensitive
                }.toString().also {
                    _receiveAddress.value = it
                    _receiveAddressUri.value = it
                }
            } else {
                // Use 2 different builders, we are restricted by spec
                // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part
                val scheme = Uri.Builder().also {
                    it.scheme(if(account.isLightning) session.bitcoin?.bip21Prefix else account.network.bip21Prefix)
                    it.opaquePart(address)
                }.toString()

                val query = Uri.Builder().also {
                    if (!amount.value.isBlank()) {
                        it.appendQueryParameter("amount", UserInput.parseUserInputSafe(
                            session = session,
                            input = amount.value,
                            assetId = account.network.policyAsset,
                            denomination = denomination.value
                        ).getBalance()?.btc)
                    }

                    if (account.network.isLiquid) {
                        it.appendQueryParameter("assetid", accountAsset.value!!.asset.assetId)
                    }
                }.toString()

                if (amount.value.isNotBlank() || !accountAsset.value?.assetId.isPolicyAsset(session)) {
                    _receiveAddress.value = scheme + query
                } else {
                    _receiveAddress.value = address
                }

                _receiveAddressUri.value = scheme + query
            }
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

            _lightningInvoice.value  = response.lnInvoice

            updateAddress(response.lnInvoice.bolt11)

            _invoiceExpirationTimestamp.value = response.lnInvoice.expireIn().toEpochMilliseconds()
            _invoiceExpiration.value = response.lnInvoice.expireIn().formatAuto()
            _invoiceDescription.value = response.lnInvoice.description
        }, onSuccess = {

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

            _onchainSwapMessage.value = getString(Res.string.id_send_more_than_s_and_up_to_s_to,
                swapInfo.minAllowedDeposit.toAmountLookOrNa(
                    session = session,
                    assetId = session.lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true,
                ),
                swapInfo.maxAllowedDeposit.toAmountLookOrNa(
                    session = session,
                    assetId = session.lightningAccount.network.policyAsset,
                    denomination = denomination.value,
                    withUnit = true
                ),
                swapInfo.channelOpeningFees?.minMsat?.satoshi()?.toAmountLook(
                    session = session,
                    assetId = account.network.policyAsset,
                    withUnit = true
                ) ?: "-"
            )

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
            val balance = amount.value.takeIf { it.isNotBlank() }?.let {
                UserInput.parseUserInputSafe(
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

            if(accountAsset.value?.account?.isLightning == true) {
                val nodeState = session.lightningSdk.nodeInfoStateFlow.value

                val openChannelFee = balance?.satoshi?.let {
                    if (it > nodeState.totalInboundLiquiditySatoshi()) session.lightningSdk.openChannelFee(
                        it
                    ) else null
                }

                _isValid.value = balance != null && (balance.satoshi >= 0 &&
                        balance.satoshi <= nodeState.maxReceivableSatoshi() &&
                        (balance.satoshi <= nodeState.totalInboundLiquiditySatoshi() || (balance.satoshi > (openChannelFee?.feeSatoshi()
                            ?: 0)))
                        )

                _amountError.value = if (amount.value.isBlank()) null else {
                    if (balance != null) {
                        val maxReceivableSatoshi = nodeState.maxReceivableSatoshi()
                        val channelMinimum = openChannelFee?.feeSatoshi() ?: 0
                        if (balance.satoshi > maxReceivableSatoshi) {
                            getString(
                                Res.string.id_you_cannot_receive_more_than_s,
                                maxReceivableSatoshi.toAmountLook(
                                    session = session,
                                    withUnit = true,
                                    denomination = denomination.value.notFiat()
                                ) ?: "", maxReceivableSatoshi.toAmountLook(
                                    session = session,
                                    withUnit = true,
                                    denomination = Denomination.fiat(session)
                                ) ?: ""
                            )
                        } else if (balance.satoshi <= channelMinimum) {
                            getString(
                                Res.string.id_this_amount_is_below_the,
                                channelMinimum.toAmountLook(
                                    session = session,
                                    withUnit = true,
                                    denomination = denomination.value.notFiat()
                                ) ?: "", channelMinimum.toAmountLook(
                                    session = session,
                                    withUnit = true,
                                    denomination = Denomination.fiat(session)
                                ) ?: ""
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                val isSetupChannel = nodeState.totalInboundLiquiditySatoshi() == 0L

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
                    amount.value.isBlank() || _amountError.value != null -> {
                        null
                    }

                    isSetupChannel -> {
                        getString(
                            Res.string.id_a_set_up_funding_fee_of_s_s,
                            channelFee,
                            channelFeeFiat
                        )
                    }

                    (balance?.satoshi ?: 0) > nodeState.totalInboundLiquiditySatoshi() -> {

                        val inboundLiquidity = nodeState.totalInboundLiquiditySatoshi().toAmountLookOrNa(
                            session = session,
                            assetId = session.lightningAccount.network.policyAsset,
                            denomination = denomination.value.notFiat(),
                            withUnit = true
                        )

                        val inboundLiquidityFiat = nodeState.totalInboundLiquiditySatoshi().toAmountLook(
                            session = session,
                            assetId = session.lightningAccount.network.policyAsset,
                            denomination = Denomination.fiat(session),
                            withUnit = true
                        ) ?: ""

                        getString(
                            Res.string.id_a_funding_fee_of_s_s_is_applied,
                            channelFee,
                            channelFeeFiat,
                            inboundLiquidity,
                            inboundLiquidityFiat
                        )
                    }

                    else -> null
                }
            }

        }, preAction = null, postAction = null, onSuccess = { }, onError = {
            _amountExchange.value = ""
            _isValid.value = false
            _amountError.value = null
            _liquidityFee.value = null
        })
    }

    private suspend fun createQRImageAndShare(address: String, data: ByteArray?) {
        if (data != null) {
            val fileSystem = platformFileSystem()

            "${_appConfig.cacheDir}/Green_QR_Code.jpeg".toPath().also {
                withContext(context = Dispatchers.IO) {
                    fileSystem.write(it) {
                        this.write(data)
                    }
                }

                postSideEffect(SideEffects.ShareFile(it))
            }
        }
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

class ReceiveViewModelPreview() : ReceiveViewModelAbstract(greenWallet = previewWallet(), accountAssetOrNull = previewAccountAsset(isLightning = true)) {

    override val receiveAddress: StateFlow<String?> = MutableStateFlow("lightning:LNBC1bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
    override val receiveAddressUri: StateFlow<String?> = MutableStateFlow("lightning:LNBC1bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
    override val amount: MutableStateFlow<String> = MutableStateFlow("")
    override val amountError: StateFlow<String?> = MutableStateFlow(null)
    override val note = MutableStateFlow("")
    override val liquidityFee: StateFlow<String?> = MutableStateFlow("A funding fee of %1 (%2) is applied when receiving amounts above your current receive capacity %3 (%4)")
    override val onchainSwapMessage: StateFlow<String?> = MutableStateFlow("Send more than %s and up to %s to this address. A minimum setup fee of %s will be applied on the received amount.\n\nThis address can be used only once.")
    override val amountCurrency: StateFlow<String> = MutableStateFlow("")
    override val amountExchange: StateFlow<String> = MutableStateFlow("")
    override val maxReceiveAmount: StateFlow<String> = MutableStateFlow("")
    override val invoiceAmountToReceive: MutableStateFlow<String?> = MutableStateFlow("123 sats")
    override val invoiceAmountToReceiveFiat: MutableStateFlow<String?> = MutableStateFlow("1.23 USD")
    override val invoiceDescription: MutableStateFlow<String?> = MutableStateFlow("Invoice Description")
    override val invoiceExpiration: MutableStateFlow<String?> = MutableStateFlow(Clock.System.now().formatAuto())
    override val invoiceExpirationTimestamp: StateFlow<Long?> = MutableStateFlow(Clock.System.now().toEpochMilliseconds())
    override val showVerifyOnDevice: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLightningOnChainAddress: StateFlow<Boolean> = MutableStateFlow(false)
    override val showAmount: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLedgerAssetWarning: StateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = ReceiveViewModelPreview()
    }
}
package com.blockstream.common.models.receive

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.at
import blockstream_green.common.generated.resources.id_address_copied_to_clipboard
import blockstream_green.common.generated.resources.id_funds_received
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.id_list_of_addresses
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_please_hold_on_while_your
import blockstream_green.common.generated.resources.id_please_wait_until_your_ledger
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_request_amount
import blockstream_green.common.generated.resources.id_send_more_than_s_and_up_to_s_to
import blockstream_green.common.generated.resources.id_sweep_from_paper_wallet
import blockstream_green.common.generated.resources.id_the_address_is_valid
import blockstream_green.common.generated.resources.id_you_have_just_received_s
import blockstream_green.common.generated.resources.lightning_fill
import blockstream_green.common.generated.resources.note_pencil
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.question
import blockstream_green.common.generated.resources.text_aa
import com.blockstream.common.AddressType
import com.blockstream.common.MediaType
import com.blockstream.common.Urls
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SupportData
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewEnrichedAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.lightning.expireIn
import com.blockstream.common.lightning.receiveAmountSatoshi
import com.blockstream.common.lightning.satoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.receive.ReceiveViewModel.LocalEvents
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.platformFileSystem
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.formatAuto
import com.blockstream.common.utils.toAmountLook
import com.blockstream.common.utils.toAmountLookOrNa
import com.blockstream.domain.boltz.BoltzUseCase
import com.blockstream.domain.hardware.VerifyAddressUseCase
import com.blockstream.domain.receive.GetReceiveAmountUseCase
import com.blockstream.domain.receive.ReceiveAmountData
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.eygraber.uri.Uri
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Serializable
sealed class PendingAction {
    data object VerifyAddress : PendingAction()
}

abstract class ReceiveViewModelAbstract(greenWallet: GreenWallet, accountAssetOrNull: AccountAsset?) :
    GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    override fun screenName(): String = "Receive"

    @NativeCoroutinesState
    abstract val showRecoveryConfirmation: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showSwap: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isReverseSubmarineSwap: MutableStateFlow<Boolean> // Lightning -> Chain

    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>

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
    abstract val receiveAmountData: StateFlow<ReceiveAmountData>

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

    @NativeCoroutinesState
    abstract val asset: StateFlow<EnrichedAsset>

    internal var pendingAction: PendingAction? = null

    fun executePendingAction() {
        (pendingAction as? PendingAction.VerifyAddress)?.also {
            postEvent(LocalEvents.VerifyOnDevice)
        }
    }
}

class ReceiveViewModel(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    ReceiveViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAsset) {
    internal val verifyAddressUseCase: VerifyAddressUseCase by inject()
    internal val boltzUseCase: BoltzUseCase by inject()
    internal val getReceiveAmountUseCase: GetReceiveAmountUseCase by inject {
        parametersOf(session, accountAsset)
    }

    private val _receiveAddress = MutableStateFlow<String?>(null)
    private val _receiveAddressUri = MutableStateFlow<String?>(null)
    private val _showVerifyOnDevice = MutableStateFlow(false)
    private val _showLightningOnChainAddress = MutableStateFlow(false)
    private val _showLedgerAssetWarning = MutableStateFlow(false)
    private val _showRequestAmount = MutableStateFlow(false)

    private val _note = MutableStateFlow<String?>(null)
    private val _liquidityFee = MutableStateFlow<String?>(null)
    private val _onchainSwapMessage = MutableStateFlow<String?>(null)
    private val _amountCurrency = MutableStateFlow("")

    private val _invoiceAmountToReceive = MutableStateFlow<String?>(null)
    private val _invoiceAmountToReceiveFiat = MutableStateFlow<String?>(null)
    private val _invoiceDescription = MutableStateFlow<String?>(null)
    private val _invoiceExpiration = MutableStateFlow<String?>(null)
    private val _invoiceExpirationTimestamp = MutableStateFlow<Long?>(null)

    override val isReverseSubmarineSwap = MutableStateFlow(false)

    override val receiveAddress: StateFlow<String?> = _receiveAddress
    override val receiveAddressUri: StateFlow<String?> = _receiveAddressUri

    override val invoiceAmountToReceive = _invoiceAmountToReceive
    override val invoiceAmountToReceiveFiat = _invoiceAmountToReceiveFiat
    override val invoiceDescription = _invoiceDescription
    override val invoiceExpiration = _invoiceExpiration
    override val invoiceExpirationTimestamp = _invoiceExpirationTimestamp

    override val amount = MutableStateFlow("")
    override val note = _note
    override val liquidityFee = _liquidityFee
    override val onchainSwapMessage = _onchainSwapMessage
    override val amountCurrency = _amountCurrency
    override val receiveAmountData = getReceiveAmountUseCase.invoke(
        amount = amount,
        denomination = denomination,
        isReverseSubmarineSwap = isReverseSubmarineSwap
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        ReceiveAmountData()
    )

    override val showVerifyOnDevice = _showVerifyOnDevice
    override val showLightningOnChainAddress = _showLightningOnChainAddress
    override val showLedgerAssetWarning = _showLedgerAssetWarning
    override val showAmount = _showRequestAmount

    private val _address = MutableStateFlow<Address?>(null)
    private val _lightningInvoicePaymentHash = MutableStateFlow<String?>(null)

    private val _appConfig: AppConfig by inject()

    private val hideWalletBackupAlert = MutableStateFlow(false)

    override val showRecoveryConfirmation: StateFlow<Boolean> =
        combine(greenWalletFlow, hideWalletBackupAlert) { greenWallet, hideWalletBackupAlert ->
            !hideWalletBackupAlert && greenWallet?.isRecoveryConfirmed == false
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    override val showSwap: StateFlow<Boolean> =
        combine(
            this@ReceiveViewModel.accountAsset.filterNotNull(),
            walletSettingsManager.getWalletSettings(walletId = greenWallet.id),
        ) { accountAsset, _ ->
            accountAsset.account.isLiquid && boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet) && accountAsset.asset.let {
                it.isLiquidPolicyAsset(
                    session
                ) && !it.isAnyAsset && !it.isAmp
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    override val asset: StateFlow<EnrichedAsset> = MutableStateFlow(accountAsset.asset)

    class LocalEvents {
        object CreateAccount : Event
        object ToggleLightning : Event
        object GenerateNewAddress : Event
        object CreateInvoice : Event
        object CopyAddress : Event
        object ShareAddress : Event
        class ShareQR(val data: ByteArray? = null) : Event
        object VerifyOnDevice : Event
        object ClearLightningInvoice : Event
        class SetNote(val note: String) : Event
        object ClickFundingFeesLearnMore : Events.OpenBrowser(Urls.HELP_FUNDING_FEES)
        object ClickLedgerSupportedAssets : Events.OpenBrowser(Urls.LEDGER_SUPPORTED_ASSETS)
        object ShowRequestAmount : Event
    }

    private val _generateAddressLock = Mutex()

    init {
        receiveAmountData.filterNotNull().onEach {
            _isValid.value = it.isValid
        }.launchIn(this)

        combine(
            showLightningOnChainAddress,
            receiveAddress,
            onProgress
        ) { showLightningOnChainAddress, receiveAddress, onProgress ->
            _navData.value = NavData(
                title = getString(Res.string.id_receive),
                subtitle = accountAsset.account.name,
                actions = listOfNotNull(
                    NavAction(
                        title = getString(Res.string.id_note),
                        icon = Res.drawable.note_pencil,
                        isMenuEntry = false,
                        onClick = {
                            postSideEffect(
                                SideEffects.NavigateTo(
                                    NavigateDestinations.Note(
                                        greenWallet = greenWallet,
                                        note = note.value ?: "",
                                        noteType = if (accountAsset.account.isLightning) NoteType.Description else NoteType.Note
                                    )
                                )
                            )
                        }
                    ).takeIf { accountAsset.account?.isLightning == true && !showLightningOnChainAddress && receiveAddress == null },
                    NavAction(
                        title = getString(Res.string.id_help),
                        icon = Res.drawable.question,
                        isMenuEntry = false,
                        onClick = {
                            postSideEffect(SideEffects.OpenBrowser(if (accountAsset.account.isAmp) Urls.HELP_AMP_ASSETS else Urls.HELP_RECEIVE_ASSETS))
                        }
                    ),

                    NavAction(
                        title = getString(Res.string.id_request_amount),
                        icon = Res.drawable.text_aa,
                        isMenuEntry = true,
                        onClick = {
                            postEvent(LocalEvents.ShowRequestAmount)
                        }
                    ).takeIf { receiveAddress.isNotBlank() && accountAsset.account.isLightning == false },
                    NavAction(
                        title = getString(Res.string.id_list_of_addresses),
                        icon = Res.drawable.at,
                        isMenuEntry = true,
                        onClick = {
                            accountAsset?.also {
                                postEvent(NavigateDestinations.Addresses(greenWallet = greenWallet, accountAsset = it))
                            }
                        }
                    ).takeIf { receiveAddress.isNotBlank() && accountAsset.account.isLightning == false },
                    NavAction(
                        title = getString(Res.string.id_sweep_from_paper_wallet),
                        icon = Res.drawable.qr_code,
                        isMenuEntry = true,
                        onClick = {
                            postEvent(NavigateDestinations.Sweep(greenWallet = greenWallet, accountAsset = accountAsset))
                        }
                    ).takeIf { receiveAddress.isNotBlank() && !accountAsset.account.isLightning && !accountAsset.account.isLiquid },
                ),
                backHandlerEnabled = onProgress
            )
        }.launchIn(this)

        sessionOrNull?.ifConnected {

            combine(
                showLightningOnChainAddress,
                isReverseSubmarineSwap
            ) { showLightningOnChainAddress, isReverseSubmarineSwap ->
                // When toggling between showLightningOnChainAddress clear the receiveAddress
                if (isReverseSubmarineSwap || (accountAsset.account.isLightning && !showLightningOnChainAddress)) {
                    _receiveAddress.value = null
                    _receiveAddressUri.value = null
                    _address.value = null
                }

                amount.value = ""
                _note.value = null
                _showRequestAmount.value = false

                _invoiceAmountToReceive.value = null
                _invoiceAmountToReceiveFiat.value = null
                _invoiceDescription.value = null
                _invoiceExpiration.value = null
            }.launchIn(this)

            combine(this@ReceiveViewModel.accountAsset, isReverseSubmarineSwap) { accountAsset, isReverseSubmarineSwap ->
                if (accountAsset == null) {
                    _showLedgerAssetWarning.value = false
                    _showVerifyOnDevice.value = false

                    _receiveAddress.value = null
                    _receiveAddressUri.value = null
                    _address.value = null
                } else if (!isReverseSubmarineSwap) {
                    generateAddress()
                }
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

                denomination
                    .onEach {
                        _amountCurrency.value =
                            it.unit(session, lightningAccount.network.policyAsset)
                    }.launchIn(viewModelScope.coroutineScope)

                session.lastInvoicePaid.filterNotNull().onEach { lastInvoicePaid ->
                    logger.d { "Last invoice paid: $lastInvoicePaid" }
                    if (lastInvoicePaid.first == _lightningInvoicePaymentHash.value) {
                        postSideEffect(
                            SideEffects.Dialog(
                                title = StringHolder.create(Res.string.id_funds_received),
                                message = StringHolder(
                                    string =
                                        getString(
                                            Res.string.id_you_have_just_received_s,
                                            lastInvoicePaid.second?.toAmountLook(
                                                session = session,
                                                withUnit = true,
                                                withGrouping = true
                                            ) ?: ""
                                        )
                                ),
                                icon = Res.drawable.lightning_fill
                            )
                        )
                        _lightningInvoicePaymentHash.value = null
                    }
                }.launchIn(viewModelScope.coroutineScope)
            }
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.CreateAccount -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.ChooseAccountType(
                            greenWallet = greenWallet,
                            assetBalance = asset.value.let { AssetBalance.create(it) },
                            allowAssetSelection = false,
                            popTo = PopTo.Receive
                        )
                    )
                )
            }

            is Events.DismissWalletBackupAlert -> {
                viewModelScope.launch {
                    hideWalletBackupAlert.value = true
                }
            }

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
                if (session.isHwWatchOnlyWithNoDevice) {
                    pendingAction = PendingAction.VerifyAddress
                    postSideEffect(
                        SideEffects.NavigateTo(NavigateDestinations.DeviceScan(greenWallet = greenWallet, isWatchOnlyDeviceConnect = true))
                    )
                } else {
                    _address.value?.also {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.DeviceInteraction(
                                    greenWalletOrNull = greenWalletOrNull,
                                    deviceId = sessionOrNull?.device?.connectionIdentifier,
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
                    addressType = if (amount.value.isBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.TEXT,
                    account = account,
                    session = session
                )
            }

            is LocalEvents.ShareAddress -> {
                countly.receiveAddress(
                    addressType = if (amount.value.isBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.TEXT,
                    isShare = true,
                    account = account,
                    session = session
                )
                postSideEffect(SideEffects.Share(receiveAddress.value ?: ""))
            }

            is LocalEvents.ShareQR -> {
                countly.receiveAddress(
                    addressType = if (amount.value.isBlank()) AddressType.ADDRESS else AddressType.URI,
                    mediaType = MediaType.IMAGE,
                    isShare = true,
                    account = account,
                    session = session
                )

                createQRImageAndShare(receiveAddress.value ?: "", event.data)
            }

            is LocalEvents.ToggleLightning -> {
                if (_showLightningOnChainAddress.value) {
                    _showLightningOnChainAddress.value = false
                } else {
                    createOnchainSwap()
                }
            }

            is LocalEvents.GenerateNewAddress -> {
                if (onProgress.value) {
                    showWaitSnackbar()
                } else {
                    generateAddress()
                }
            }

            is Events.NavigateBackUserAction -> {
                showWaitSnackbar()
            }

            is LocalEvents.CreateInvoice -> {
                createLightningInvoice()
            }

            is LocalEvents.SetNote -> {
                _note.value = event.note
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

    override fun errorReport(exception: Throwable): SupportData {
        return SupportData.create(throwable = exception, network = account.network, session = session)
    }

    private fun verifyAddressOnDevice() {
        _address.value?.let { address ->
            doAsync(
                {
                    verifyAddressUseCase.invoke(
                        session = session,
                        account = account,
                        address = address
                    )
                }, mutex = getMutex("verifyAddressOnDevice"), timeout = 1L.minutes,
                preAction = null, postAction = null, onSuccess = {
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_the_address_is_valid)))
                    postSideEffect(SideEffects.Dismiss)
                }, onError = {
                    postSideEffect(SideEffects.ErrorDialog(it))
                    postSideEffect(SideEffects.Dismiss)
                })
        }
    }

    private fun generateAddress() {
        logger.i { "Generating address for ${account.name}" }
        _showLedgerAssetWarning.value = account.isLiquid && session.device?.isLedger == true
        _showVerifyOnDevice.value = session.device?.canVerifyAddressOnDevice(account) ?: session.isHwWatchOnly

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

            if ((isReverseSubmarineSwap.value || account.isLightning) && !showLightningOnChainAddress.value) {
                val scheme = if (isReverseSubmarineSwap.value) "lightning" else account.network.bip21Prefix
                Uri.Builder().also {
                    it.scheme(scheme)
                    it.opaquePart(address.uppercase()) // bech32 is case insensitive
                }.toString().also {
                    _receiveAddress.value = it
                    _receiveAddressUri.value = it
                }
            } else {
                // Use 2 different builders, we are restricted by spec
                // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part
                val scheme = Uri.Builder().also {
                    it.scheme(if (account.isLightning) session.bitcoin?.bip21Prefix else account.network.bip21Prefix)
                    it.opaquePart(address)
                }.toString()

                val query = Uri.Builder().also {

                    if (!amount.value.isBlank()) {
                        it.appendQueryParameter(
                            "amount", UserInput.parseUserInputSafe(
                                session = session,
                                input = amount.value,
                                assetId = asset.value.assetId,
                                denomination = denomination.value
                            ).getBalance()?.btc
                        )
                    }

                    if (account.network.isLiquid) {
                        it.appendQueryParameter("assetid", this@ReceiveViewModel.accountAsset.value!!.asset.assetId)
                    }
                }.toString()

                if (amount.value.isNotBlank() || !this@ReceiveViewModel.accountAsset.value?.assetId.isPolicyAsset(session)) {
                    _receiveAddress.value = scheme + query
                } else {
                    _receiveAddress.value = address
                }

                _receiveAddressUri.value = scheme + query
            }
        }
    }

    private fun createLightningInvoice() {
        doAsync({
            val amount = UserInput.parseUserInput(
                session = session,
                input = amount.value,
                assetId = account.network.policyAsset,
                denomination = denomination.value
            ).getBalance()?.satoshi ?: 0

            if (isReverseSubmarineSwap.value) {

                val invoice = boltzUseCase.createReverseSubmarineSwapUseCase(
                    wallet = greenWallet,
                    session = session,
                    account = account,
                    amount = amount,
                    description = null
                )

                // Fee is lockup fee + boltz fee + claim fee. We hardcode claim value for 1in 1out tx
                val fee = (invoice.fee()?.toLong() ?: 0) + 22

                val bolt11 = invoice.bolt11Invoice()

                val receiveAmount = (bolt11.amountMilliSatoshis()?.satoshi() ?: 0) - fee

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

                _lightningInvoicePaymentHash.value = bolt11.paymentHash()

                updateAddress(bolt11.toString())

                _invoiceExpirationTimestamp.value = bolt11.expireIn().toEpochMilliseconds()
                _invoiceExpiration.value = bolt11.expireIn().formatAuto()
                _invoiceDescription.value = bolt11.invoiceDescription()

            } else {
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

                _lightningInvoicePaymentHash.value = response.lnInvoice.paymentHash

                updateAddress(response.lnInvoice.bolt11)

                _invoiceExpirationTimestamp.value = response.lnInvoice.expireIn().toEpochMilliseconds()
                _invoiceExpiration.value = response.lnInvoice.expireIn().formatAuto()
                _invoiceDescription.value = response.lnInvoice.description
            }
        }, onSuccess = {

        })
    }

    private fun createOnchainSwap() {
        doAsync({
            val swapInfo = session.receiveOnchain()
            swapInfo to (swapInfo.channelOpeningFees?.minMsat?.satoshi()?.toAmountLook(
                session = session,
                assetId = account.network.policyAsset,
                withUnit = true
            ) ?: "-")

            _onchainSwapMessage.value = getString(
                Res.string.id_send_more_than_s_and_up_to_s_to,
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
                assetId = this@ReceiveViewModel.accountAsset.value?.assetId,
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

class ReceiveViewModelPreview() :
    ReceiveViewModelAbstract(greenWallet = previewWallet(), accountAssetOrNull = previewAccountAsset(isLightning = true)) {

    override val showRecoveryConfirmation: StateFlow<Boolean> = MutableStateFlow(false)
    override val showSwap: StateFlow<Boolean> = MutableStateFlow(true)
    override val isReverseSubmarineSwap = MutableStateFlow(true)
    override val receiveAddress: StateFlow<String?> =
        MutableStateFlow("lightning:LNBC1bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
    override val receiveAddressUri: StateFlow<String?> =
        MutableStateFlow("lightning:LNBC1bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gkh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznubc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
    override val amount: MutableStateFlow<String> = MutableStateFlow("")
    override val note = MutableStateFlow("")
    override val liquidityFee: StateFlow<String?> =
        MutableStateFlow("A funding fee of %1 (%2) is applied when receiving amounts above your current receive capacity %3 (%4)")
    override val onchainSwapMessage: StateFlow<String?> =
        MutableStateFlow("Send more than %s and up to %s to this address. A minimum setup fee of %s will be applied on the received amount.\n\nThis address can be used only once.")
    override val amountCurrency: StateFlow<String> = MutableStateFlow("")
    override val receiveAmountData: StateFlow<ReceiveAmountData> = MutableStateFlow(ReceiveAmountData())
    override val invoiceAmountToReceive: MutableStateFlow<String?> = MutableStateFlow("123 sats")
    override val invoiceAmountToReceiveFiat: MutableStateFlow<String?> = MutableStateFlow("1.23 USD")
    override val invoiceDescription: MutableStateFlow<String?> = MutableStateFlow("Invoice Description")
    override val invoiceExpiration: MutableStateFlow<String?> = MutableStateFlow(Clock.System.now().formatAuto())
    override val invoiceExpirationTimestamp: StateFlow<Long?> = MutableStateFlow(Clock.System.now().toEpochMilliseconds())
    override val showVerifyOnDevice: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLightningOnChainAddress: StateFlow<Boolean> = MutableStateFlow(false)
    override val showAmount: StateFlow<Boolean> = MutableStateFlow(false)
    override val showLedgerAssetWarning: StateFlow<Boolean> = MutableStateFlow(false)
    override val asset: StateFlow<EnrichedAsset> = MutableStateFlow(previewEnrichedAsset())

    companion object {
        fun preview() = ReceiveViewModelPreview()
    }
}

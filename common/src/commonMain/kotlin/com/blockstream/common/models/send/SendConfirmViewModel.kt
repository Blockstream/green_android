package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_review
import blockstream_green.common.generated.resources.id_sign_transaction
import blockstream_green.common.generated.resources.id_the_address_is_valid
import blockstream_green.common.generated.resources.note_pencil
import blockstream_green.common.generated.resources.signature
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.Banner
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString


abstract class SendConfirmViewModelAbstract(greenWallet: GreenWallet, accountAsset: AccountAsset) :
    CreateTransactionViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAsset) {
    override fun screenName(): String = "SendConfirm"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.accountSegmentation(
            session = session,
            account = account
        )
    }

    @NativeCoroutinesState
    abstract val transactionConfirmLook: StateFlow<TransactionConfirmLook?>

    @NativeCoroutinesState
    abstract val showVerifyOnDevice: StateFlow<Boolean>
}

class SendConfirmViewModel constructor(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    denomination: Denomination?
) : SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    private val _showVerifyOnDevice = MutableStateFlow(false)
    override val showVerifyOnDevice = _showVerifyOnDevice

    private val _transactionConfirmLook: MutableStateFlow<TransactionConfirmLook?> =
        MutableStateFlow(null)

    override val transactionConfirmLook: StateFlow<TransactionConfirmLook?> =
        _transactionConfirmLook.asStateFlow()

    class LocalEvents {
        object Note : Event
        object VerifyOnDevice: Event
    }

    init {
        onProgressSending.onEach { onProgressSending ->
            _navData.value = NavData(
                title = getString(Res.string.id_review), subtitle = greenWallet.name,
                isVisible = !onProgressSending,
                actions = listOfNotNull(
                    NavAction(
                        title = getString(Res.string.id_add_note),
                        icon = Res.drawable.note_pencil,
                        isMenuEntry = false
                    ) {
                        postEvent(LocalEvents.Note)
                    },
                    (NavAction(
                        title = getString(Res.string.id_sign_transaction),
                        icon = Res.drawable.signature,
                        isMenuEntry = true
                    ) {
                        postEvent(
                            CreateTransactionViewModelAbstract.LocalEvents.SignTransaction(
                                broadcastTransaction = false
                            )
                        )
                    }).takeIf { appInfo.isDevelopmentOrDebug },
                )
            )
        }.launchIn(this)

        session.ifConnected {
            if (denomination != null && !denomination.isFiat) {
                _denomination.value = denomination
            }

            session.pendingTransaction?.also {
                viewModelScope.coroutineScope.launch {
                    if (appInfo.isDevelopmentOrDebug) {
                        logger.d { "Params: ${it.params}" }
                        logger.d { "Transaction: ${it.transaction}" }
                    }

                    _transactionConfirmLook.value = TransactionConfirmLook.create(
                        params = it.params,
                        transaction = it.transaction,
                        account = account,
                        session = session,
                        denomination = _denomination.value,
                        isAddressVerificationOnDevice = false
                    )

                    _showVerifyOnDevice.value =
                        if (it.params.isRedeposit) session.device?.canVerifyAddressOnDevice(account)
                            ?: false else false

                    _isValid.value = true
                }

            } ?: run {
                postSideEffect(SideEffects.NavigateBack())
            }
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is CreateTransactionViewModelAbstract.LocalEvents.SignTransaction -> {
                session.pendingTransaction?.also {
                    signAndSendTransaction(
                        params = it.params,
                        originalTransaction = it.transaction,
                        segmentation = it.segmentation,
                        broadcast = event.broadcastTransaction,
                        createPsbt = event.createPsbt
                    )
                }
            }

            is CreateTransactionViewModelAbstract.LocalEvents.BroadcastTransaction -> {
                session.pendingTransaction?.also {
                    signAndSendTransaction(
                        params = it.params,
                        originalTransaction = it.transaction,
                        segmentation = it.segmentation,
                        psbt = event.psbt,
                        broadcast = event.broadcastTransaction,
                    )
                }
            }

            is LocalEvents.Note -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Note(greenWallet = greenWallet, note = note.value, noteType = NoteType.Note)))
            }

            is LocalEvents.VerifyOnDevice -> {
                verifyAddressOnDevice()
            }
        }
    }

    private fun verifyAddressOnDevice() {
        val hwWallet = session.gdkHwWallet
        val transaction = session.pendingTransaction?.transaction

        if(hwWallet != null && transaction != null){
            doAsync({
                countly.verifyAddress(session, account)

                transaction.outputs.filter { it.address.isNotBlank() }.forEach { output ->
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.DeviceInteraction(
                                greenWalletOrNull = greenWalletOrNull,
                                deviceId = session.device?.connectionIdentifier,
                                verifyAddress = output.address
                            )
                        )
                    )

                    if(hwWallet.getGreenAddress(
                            network = account.network,
                            hwInteraction = null,
                            account = account,
                            path = output.userPath ?: listOf(),
                            csvBlocks = output.subType ?: 0
                        ) != output.address){
                        throw Exception("id_the_addresses_dont_match")
                    }

                    postSideEffect(SideEffects.Dismiss)
                }

            }, preAction = null, postAction = null, onSuccess = {
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

    companion object: Loggable()
}

class SendConfirmViewModelPreview(
    greenWallet: GreenWallet,
    transactionConfirmLook: TransactionConfirmLook? = null
) : SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = previewAccountAsset()) {

    override val transactionConfirmLook: StateFlow<TransactionConfirmLook?> =
        MutableStateFlow(transactionConfirmLook)

    override val showVerifyOnDevice: StateFlow<Boolean> = MutableStateFlow(false)

    init {
        banner.value = Banner.preview3
        note.value = "Note"
    }

    companion object {
        fun preview() = SendConfirmViewModelPreview(
            previewWallet(), transactionConfirmLook = TransactionConfirmLook(
                from = previewAccountAsset(),
                utxos = listOf(
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        assetId = BTC_POLICY_ASSET,
                        amount = "2.123 BTC",
                        amountExchange = "45.123 USD"
                    )
                ),
                fee = "0.0123 BTC",
                feeFiat = "13,03 USD",
                feeRate = "1 vbyte/sats",
                total = "5.5 BTC",
                totalFiat = "143,234 USD"
            )
        )

        fun previewAccountExchange() = SendConfirmViewModelPreview(
            previewWallet(), transactionConfirmLook = TransactionConfirmLook(
                from = previewAccountAsset(),
                to = previewAccountAsset(),
                amount = "2.123 BTC",
                amountFiat = "43.312 USD",
                fee = "0.0123 BTC",
                feeFiat = "13,03 USD",
                feeRate = "1 vbyte/sats",
                total = "5.5 BTC",
                totalFiat = "143,234 USD"
            )
        )
    }
}
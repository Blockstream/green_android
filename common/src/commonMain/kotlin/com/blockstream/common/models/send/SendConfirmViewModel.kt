package com.blockstream.common.models.send

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_note
import blockstream_green.common.generated.resources.id_review
import blockstream_green.common.generated.resources.id_sign_transaction
import blockstream_green.common.generated.resources.note_pencil
import blockstream_green.common.generated.resources.signature
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.Banner
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.sheets.NoteType
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}

class SendConfirmViewModel constructor(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset,
    denomination: Denomination?
) : SendConfirmViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {

    private val _transactionConfirmLook: MutableStateFlow<TransactionConfirmLook?> =
        MutableStateFlow(null)

    override val transactionConfirmLook: StateFlow<TransactionConfirmLook?> =
        _transactionConfirmLook.asStateFlow()

    class LocalEvents {
        object Note : Event
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_review), subtitle = greenWallet.name,
                onBackPressed = {
                    !(onProgress.value)
                },
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
        }

        session.ifConnected {
            if (denomination != null && !denomination.isFiat) {
                _denomination.value = denomination
            }

            session.pendingTransaction?.also {
                viewModelScope.coroutineScope.launch {
                    if (appInfo.isDevelopmentOrDebug) {
                        logger.d { "Params: ${it.first}" }
                        logger.d { "Transaction: ${it.second}" }
                    }

                    _transactionConfirmLook.value = TransactionConfirmLook.create(
                        params = it.first,
                        transaction = it.second,
                        account = account,
                        session = session,
                        denomination = _denomination.value,
                        isAddressVerificationOnDevice = false
                    )

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
                        originalParams = it.first,
                        originalTransaction = it.second,
                        segmentation = it.third,
                        broadcast = event.broadcastTransaction
                    )
                }
            }

            is LocalEvents.Note -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Note(note = note.value, noteType = NoteType.Note)))
            }
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
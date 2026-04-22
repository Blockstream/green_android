package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_address_was_filled_by_a_payment
import blockstream_green.common.generated.resources.id_send
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.compose.utils.getStringFromId
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.banner.Banner
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.domain.send.SendFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

abstract class SendAddressViewModelAbstract(greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null) :
    CreateTransactionViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "SendAddress"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    abstract val address: MutableStateFlow<String>
}

class SendAddressViewModel(
    greenWallet: GreenWallet,
    initAddress: String? = null,
    addressType: AddressInputType? = null,
    initialAccountAsset: AccountAsset? = null
) : SendAddressViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = initialAccountAsset) {

    override val address: MutableStateFlow<String> = MutableStateFlow(initAddress?.trim().orEmpty())

    private val sendFlow: MutableStateFlow<SendFlow?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                isCentered = true,
                subtitle = greenWallet.name,
                title = getString(Res.string.id_send),
            )
        }

        _addressInputType = addressType

        session.ifConnected {
            sessionManager.pendingUri.filterNotNull().onEach {
                sessionManager.pendingUri.value = null
                address.value = it.trim()
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_address_was_filled_by_a_payment)))
            }.launchIn(this)

            // Debounce per-keystroke address changes. checkAddress hits parseInput, which now
            // does DNS (BIP-353 auto-try) and HTTPS (LNURL) on email-shaped inputs — without
            // a debounce, every typed character triggers a fresh wave of network calls.
            // mapLatest cancels the in-flight checkAddress when a new value arrives so the user
            // can't navigate forward against a stale input that finished resolving after they
            // moved on (see proceedToSendFlow side-effect inside checkAddress).
            @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
            address
                .debounce(ADDRESS_INPUT_DEBOUNCE_MS)
                .mapLatest {
                    withContext(context = Dispatchers.Default) {
                        checkAddress(it)
                    }
                }
                .launchIn(this)
        }

        sendFlow.onEach {
            _isValid.value = it != null
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            sendFlow.value?.also {
                proceedToSendFlow(it)
            }
        }
    }

    private suspend fun checkAddress(address: String) {
        _error.value = null
        sendFlow.value = null

        if (address.isBlank()) {
            return
        }

        // Drive the top progress bar while getSendFlowUseCase runs. It triggers the
        // BIP-353 + LNURL race inside Lwk which can take several seconds on slow DNS,
        // so the user needs a visible signal that work is in flight.
        onProgress.value = true
        try {
            val sendFlow = sendUseCase.getSendFlowUseCase(
                greenWallet = greenWallet,
                session = session,
                address = address,
                asset = accountAsset.value?.asset,
                account = accountAsset.value
            ).also {
                sendFlow.value = it
            }

            proceedToSendFlow(sendFlow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = e.message?.let { getStringFromId(it) }
        } finally {
            onProgress.value = false
        }
    }

    private fun proceedToSendFlow(sendFlow: SendFlow) {
        when (sendFlow) {
            is SendFlow.SelectAsset -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendChooseAsset(
                            greenWallet = greenWallet,
                            address = sendFlow.address,
                            addressType = _addressInputType ?: AddressInputType.PASTE,
                            assets = sendFlow.assets,
                        )
                    )
                )
            }

            is SendFlow.SelectAccount -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendChooseAccount(
                            greenWallet = greenWallet,
                            address = sendFlow.address,
                            addressType = _addressInputType ?: AddressInputType.PASTE,
                            asset = sendFlow.asset,
                            accounts = sendFlow.accounts,
                        )
                    )
                )
            }

            is SendFlow.SelectAmount -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Send(
                            greenWallet = greenWallet,
                            address = sendFlow.address,
                            addressType = _addressInputType ?: AddressInputType.PASTE,
                            accountAsset = sendFlow.account,
                        )
                    )
                )
            }

            is SendFlow.SelectLightningAmount -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendLightningAmount(
                            greenWallet = greenWallet,
                            address = sendFlow.address,
                            addressType = _addressInputType ?: AddressInputType.PASTE,
                            accountAsset = sendFlow.account,
                        )
                    )
                )
            }

            is SendFlow.SendConfirmation -> {
                session.pendingTransaction = PendingTransaction(
                    params = sendFlow.params,
                    transaction = sendFlow.transaction,
                    segmentation = TransactionSegmentation(
                        transactionType = TransactionType.SEND,
                        addressInputType = _addressInputType,
                        sendAll = false
                    )
                )
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendConfirm(
                            greenWallet = greenWallet,
                            accountAsset = sendFlow.account,
                            denomination = denomination.value
                        )
                    )
                )
            }

            is SendFlow.SendLightningConfirmation -> {
                session.pendingTransaction = PendingTransaction(
                    params = sendFlow.params,
                    transaction = sendFlow.transaction,
                    segmentation = TransactionSegmentation(
                        transactionType = TransactionType.SEND,
                        addressInputType = _addressInputType,
                        sendAll = false
                    )
                )
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendLightningConfirm(
                            greenWallet = greenWallet,
                            accountAsset = sendFlow.account,
                            invoice = sendFlow.invoice,
                            amountSatoshi = sendFlow.params.swap?.toAmount,
                            denomination = denomination.value
                        )
                    )
                )
            }
        }
    }

    companion object {
        private const val ADDRESS_INPUT_DEBOUNCE_MS = 300L
    }
}

class SendAddressViewModelPreview(greenWallet: GreenWallet) :
    SendAddressViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = null) {

    override val address: MutableStateFlow<String> = MutableStateFlow("address")

    init {
        _showFeeSelector.value = true
        banner.value = Banner.preview3
    }

    companion object {
        fun preview(isLightning: Boolean = false) = SendAddressViewModelPreview(previewWallet())
    }
}

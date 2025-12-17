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
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.banner.Banner
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.domain.send.SendFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
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

    override val address: MutableStateFlow<String> = MutableStateFlow(initAddress ?: "")

    private val sendFlow: MutableStateFlow<SendFlow?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_send),
            )
        }

        _addressInputType = addressType

        session.ifConnected {
            sessionManager.pendingUri.filterNotNull().onEach {
                sessionManager.pendingUri.value = null
                address.value = it
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_address_was_filled_by_a_payment)))
            }.launchIn(this)

            address.onEach {
                withContext(context = Dispatchers.Default) {
                    checkAddress(it)
                }
            }.launchIn(this)
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

        if (address.isBlank()) {
            return
        }

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
        } catch (e: Exception) {
            _error.value = e.message
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
        }
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

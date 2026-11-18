package com.blockstream.compose.models.lightning

import com.blockstream.compose.utils.StringHolder
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_address_copied_to_clipboard
import blockstream_green.common.generated.resources.id_lightning_invoice
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.AddressType
import com.blockstream.data.MediaType
import com.blockstream.data.Urls
import com.blockstream.data.data.GreenWallet
import com.blockstream.domain.receive.SaveAndShareQrCodeUseCase
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

data class LightningInvoiceState(
    val invoiceUri: String,
    val amount: String,
    val amountFiat: String?,
    val feeText: String? = null,
    val feeFiatText: String? = null,
    val description: String?,
    val expiration: String?,
    val isSwap: Boolean = false
)

abstract class LightningInvoiceViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "LightningInvoiceDetails"
    abstract val state: LightningInvoiceState
}

class LightningInvoiceViewModel(
    greenWallet: GreenWallet,
    override val state: LightningInvoiceState
) : LightningInvoiceViewModelAbstract(greenWallet = greenWallet) {

    private val saveAndShareQrCodeUseCase: SaveAndShareQrCodeUseCase by inject()

    sealed class LocalEvents {
        object CopyAddress : Event
        object ShareAddress : Event
        object ClickFundingFee : Event
        object ClickFundingFeeLearnMore : Event
        class ShareQR(val qrBytes: ByteArray) : Event
    }

    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_lightning_invoice),
            isCentered = true
        )

        this.bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is LocalEvents.CopyAddress -> {
                postSideEffect(
                    SideEffects.CopyToClipboard(
                        value = state.invoiceUri,
                        message = getString(Res.string.id_address_copied_to_clipboard),
                        label = "Invoice"
                    )
                )

                // Native Lightning invoices only
                if (!state.isSwap) {
                    countly.receiveAddress(
                        addressType = AddressType.URI,
                        mediaType = MediaType.TEXT,
                        account = session.lightningAccount,
                        session = session
                    )
                }
            }

            is LocalEvents.ShareAddress -> {
                postSideEffect(SideEffects.Share(state.invoiceUri))

                if (!state.isSwap) {
                    countly.receiveAddress(
                        addressType = AddressType.URI,
                        mediaType = MediaType.TEXT,
                        isShare = true,
                        account = session.lightningAccount,
                        session = session
                    )
                }
            }

            is LocalEvents.ClickFundingFee -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.LightningFeeInfo))
            }

            is LocalEvents.ClickFundingFeeLearnMore -> {
                postSideEffect(SideEffects.OpenBrowser(Urls.HELP_FUNDING_FEES))
            }

            is LocalEvents.ShareQR -> {
                saveAndShareQrCodeUseCase(event.qrBytes)?.also { cachePath ->
                    postSideEffect(SideEffects.ShareFile(cachePath))
                }

                if (!state.isSwap) {
                    countly.receiveAddress(
                        addressType = AddressType.URI,
                        mediaType = MediaType.IMAGE,
                        isShare = true,
                        account = session.lightningAccount,
                        session = session
                    )
                }
            }
        }
    }
}

class LightningInvoiceViewModelPreview : LightningInvoiceViewModelAbstract(
    greenWallet = com.blockstream.compose.extensions.previewWallet()
) {
    override val state = LightningInvoiceState(
        invoiceUri = "lightning:lnbc1...",
        amount = "21,000 sats",
        amountFiat = "$14.50",
        description = "For coffee",
        feeText = "2,500 sats",
        feeFiatText = "$1.70",
        expiration = "Expires in 59 minutes"
    )
}
package com.blockstream.compose.models.lightning

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_address_copied_to_clipboard
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.domain.receive.SaveAndShareQrCodeUseCase
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

data class LightningInvoiceState(
    val invoiceUri: String,
    val amount: String,
    val amountFiat: String?,
    val description: String?,
    val expiration: String?
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
        class ShareQR(val qrBytes: ByteArray) : Event
    }

    init {
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
            }

            is LocalEvents.ShareAddress -> {
                postSideEffect(SideEffects.Share(state.invoiceUri))
            }

            is LocalEvents.ShareQR -> {
                saveAndShareQrCodeUseCase(event.qrBytes)?.also { cachePath ->
                    postSideEffect(SideEffects.ShareFile(cachePath))
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
        expiration = "Expires in 59 minutes"
    )
}
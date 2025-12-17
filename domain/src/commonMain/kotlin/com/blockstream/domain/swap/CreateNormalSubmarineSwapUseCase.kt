package com.blockstream.domain.swap

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.letTryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.swap.SwapDetails
import lwk.Bolt11Invoice
import lwk.LwkException

/**
 * Creates or restores a Normal Submarine Swap for a given BOLT11 invoice.
 *
 * A Normal Submarine Swap allows paying a Lightning invoice by funding an on‑chain transaction.
 * If there is already a stored swap for the provided invoice, this use case will attempt to
 * restore it; otherwise it will create a new one via LWK.
 */
class CreateNormalSubmarineSwapUseCase(val database: Database) {
    /**
     * Creates or restores the swap and persists its state in the database.
     *
     * This method handles the lifecycle of a submarine swap:
     * 1. Normalizes the invoice by removing the `lightning:` prefix.
     * 2. Checks the database for an existing swap associated with this invoice and the current xpub.
     * 3. If found, attempts to restore the preparation data via LWK.
     * 4. If not found, requests LWK to create a new submarine swap.
     * 5. Special Case: If LWK reports a `MagicRoutingHint`, it returns immediate funding details.
     * 6. Persists the resulting swap data to the database for future tracking.
     *
     * @param wallet the active [GreenWallet] identifying the user's wallet
     * @param session the current [GdkSession] providing access to the LWK and network details
     * @param account the on‑chain [Account] that will be used to fund the swap transaction
     * @param invoice the BOLT11 invoice to be paid (can include `lightning:` prefix)
     * @return [SwapDetails] describing the funding address and required amount
     * @throws Exception if the invoice is missing an amount or if required session data is unavailable
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        isAutoSwap: Boolean,
        account: Account,
        invoice: String
    ): SwapDetails {

        val xPubHashId = session.xPubHashId ?: throw Exception("xPubHashId should not be null")

        val invoice = invoice.replace("lightning:", "")

        val bolt11Invoice = Bolt11Invoice(invoice)

        if (bolt11Invoice.amountMilliSatoshis() == null) {
            throw Exception("id_no_amount_less_invoices_supported")
        }

        val swap = database.getSwapFromInvoice(
            invoice = invoice,
            xPubHashId = xPubHashId
        )?.letTryCatch {
            session.lwk.restorePreparePay(it.data_)
        } ?: try {
            session.lwk.createNormalSubmarineSwap(bolt11Invoice = invoice, refundAddress = session.getReceiveAddress(account).address)
        } catch (hint: LwkException.MagicRoutingHint) {
            return SwapDetails(
                swapId = null,
                address = hint.address,
                fromAmount = hint.amount.toLong(),
                submarineInvoiceTo = invoice,
                fromAssetId = account.network.policyAsset
            )
        }

        // Save swap
        database.setSwap(
            id = swap.swapId(),
            walletId = wallet.id,
            xPubHashId = xPubHashId,
            invoice = invoice,
            swapType = SwapType.NormalSubmarine,
            isAutoSwap = isAutoSwap,
            data = swap.serialize()
        )

        return SwapDetails(
            swapId = swap.swapId(),
            address = swap.uriAddress().toString(),
            fromAmount = swap.uriAmount().toLong(),
            submarineInvoiceTo = invoice,
            toAmount = bolt11Invoice.amountMilliSatoshis()?.satoshi() ?: 0,
            providerFee = swap.boltzFee()?.toLong() ?: 0,
            claimNetworkFee = (swap.fee()?.toLong() ?: 0) - (swap.boltzFee()?.toLong() ?: 0),
            fromAssetId = account.network.policyAsset
        )
    }
}

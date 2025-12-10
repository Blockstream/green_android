package com.blockstream.domain.boltz

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.letTryCatch
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.green.data.lwk.NormalSubmarineSwap
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
     * Behavior:
     * - Strips a leading `lightning:` prefix from [invoice] if present.
     * - Tries to restore an existing swap from the database for the current xpub.
     * - Falls back to creating a new swap through `session.lwk.createNormalSubmarineSwap`.
     * - When LWK throws [lwk.LwkException.MagicRoutingHint], returns a lightweight
     *   [NormalSubmarineSwap] with the hinted address and amount so the caller can fund it.
     * - Persists the (restored or newly created) swap in the database before returning.
     *
     * @param wallet the active [GreenWallet]
     * @param session the current [GdkSession] providing xpub and LWK access
     * @param account the on‑chain [Account] that will fund the swap
     * @param invoice the BOLT11 invoice (with or without `lightning:` prefix)
     * @return a [NormalSubmarineSwap] describing the prepared payment/funding details
     * @throws Exception if `session.xPubHashId` is null when required
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        account: Account,
        invoice: String
    ): NormalSubmarineSwap {

        val invoice = invoice.replace("lightning:", "")

        val bolt11Invoice = Bolt11Invoice(invoice)

        if (bolt11Invoice.amountMilliSatoshis() == null) {
            throw Exception("id_no_amount_less_invoices_supported")

        }

        val pay = database.getSwapFromInvoice(
            invoice = invoice,
            xPubHashId = session.xPubHashId ?: throw Exception("xPubHashId should not be null")
        )?.letTryCatch {
            session.lwk.restorePreparePay(it.data_)
        } ?: try {
            session.lwk.createNormalSubmarineSwap(bolt11Invoice = invoice, refundAddress = session.getReceiveAddress(account).address)
        } catch (hint: LwkException.MagicRoutingHint) {
            return NormalSubmarineSwap(
                address = hint.address,
                satoshi = hint.amount.toLong(),
                bolt11Invoice = invoice
            )
        }

        // Save swap
        database.setSwap(
            id = pay.swapId(),
            walletId = wallet.id,
            xPubHashId = session.xPubHashId ?: throw Exception("xPubHashId should not be null"),
            invoice = invoice,
            data = pay.serialize()
        )

        return NormalSubmarineSwap.from(invoice = invoice, pay = pay)
    }
}

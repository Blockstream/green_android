package com.blockstream.domain.boltz

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import lwk.InvoiceResponse

/**
 * Creates a Reverse Submarine Swap invoice to receive Lightning by depositing on‑chain.
 *
 * A Reverse Submarine Swap issues a BOLT11 invoice that will be settled once the user funds a
 * specified on‑chain address with the required amount. The swap metadata is persisted so it can be
 * tracked and resumed by background workers.
 */
class CreateReverseSubmarineSwapUseCase(
    private val database: Database
) {
    /**
     * Creates a reverse swap invoice via LWK and persists it to the database.
     *
     * @param wallet the active [GreenWallet]
     * @param session the current [GdkSession]
     * @param account the on‑chain [Account] providing the receive address
     * @param amount the amount to receive (in satoshis)
     * @param description optional invoice description/metadata
     * @return the created [InvoiceResponse] containing the BOLT11 invoice and swap id
     * @throws Exception if `session.xPubHashId` is null when persisting
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        account: Account,
        amount: Long,
        description: String?
    ): InvoiceResponse {

        val invoice = session.lwk.createReverseSubmarineSwap(
            address = session.getReceiveAddress(account).address,
            amount = amount,
            description = description
        )

        database.setSwap(
            id = invoice.swapId(),
            walletId = wallet.id,
            xPubHashId = session.xPubHashId ?: throw Exception("xPubHashId should not be null"),
            data = invoice.serialize()
        )

        return invoice
    }
}

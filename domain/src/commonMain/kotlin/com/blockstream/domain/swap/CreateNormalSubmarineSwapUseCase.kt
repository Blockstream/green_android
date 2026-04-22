@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.domain.swap

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.letTryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.data.swap.SwapDetails
import lwk.Bolt11Invoice
import lwk.LwkException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        invoice: String,
        amountSats: Long? = null,
    ): SwapDetails {

        val xPubHashId = session.xPubHashId ?: throw Exception("xPubHashId should not be null")
        val normalizedInput = invoice.replace("lightning:", "")
        val refundAddress = session.getReceiveAddress(account).address

        val instruction = try {
            session.lwkOrNull?.inspectPaymentInstruction(normalizedInput)
        } catch (_: Exception) {
            null
        }

        if (instruction is PaymentInstruction.LnUrl || instruction is PaymentInstruction.Bolt12) {
            return createForInstruction(
                wallet = wallet,
                session = session,
                isAutoSwap = isAutoSwap,
                account = account,
                input = normalizedInput,
                amountSats = amountSats,
                refundAddress = refundAddress,
                xPubHashId = xPubHashId,
            )
        }

        // BOLT11 path (existing behaviour, unchanged)
        val bolt11Invoice = Bolt11Invoice(normalizedInput)

        if (bolt11Invoice.amountMilliSatoshis() == null) {
            throw Exception("id_no_amount_less_invoices_supported")
        }

        // Prevent double spending even when paid using Magic hint routing
        if (database.isInvoicePaid(invoice = normalizedInput)) {
            throw Exception("id_invoice_already_paid")
        }

        val boltzSwap = database.getSwapFromUnpaidInvoice(
            invoice = normalizedInput,
            xPubHashId = xPubHashId
        )

        val swap = boltzSwap?.takeIf { !it.is_magic }?.letTryCatch {
            session.lwk.restorePreparePay(it.data_)
        } ?: try {
            session.lwk.createNormalSubmarineSwap(input = normalizedInput, refundAddress = refundAddress)
        } catch (hint: LwkException.MagicRoutingHint) {
            val swap = SwapDetails(
                swapId = Uuid.generateV7().toString(),
                address = hint.address,
                fromAmount = hint.amount.toLong(),
                fromAssetId = account.network.policyAsset,
                toAmount = hint.amount.toLong(),
                toAssetId = session.lightning.policyAsset,
                submarineInvoiceTo = normalizedInput
            )

            if (boltzSwap == null) {
                // Save Magic Swap if not already exists
                database.setSwap(
                    id = swap.swapId,
                    walletId = wallet.id,
                    xPubHashId = xPubHashId,
                    invoice = normalizedInput,
                    swapType = SwapType.NormalSubmarine,
                    isAutoSwap = isAutoSwap,
                    isMagic = true,
                    data = ""
                )
            }

            return swap
        }

        // Save swap
        database.setSwap(
            id = swap.swapId(),
            walletId = wallet.id,
            xPubHashId = xPubHashId,
            invoice = normalizedInput,
            swapType = SwapType.NormalSubmarine,
            isAutoSwap = isAutoSwap,
            isMagic = false,
            data = swap.serialize()
        )

        return SwapDetails(
            swapId = swap.swapId(),
            address = swap.uriAddress().toString(),
            fromAmount = swap.uriAmount().toLong(),
            fromAssetId = account.network.policyAsset,
            submarineInvoiceTo = normalizedInput,
            toAmount = bolt11Invoice.amountMilliSatoshis()?.satoshi() ?: 0,
            toAssetId = session.lightning.policyAsset,
            providerFee = swap.boltzFee()?.toLong() ?: 0,
            claimNetworkFee = (swap.fee()?.toLong() ?: 0) - (swap.boltzFee()?.toLong() ?: 0)
        )
    }

    private suspend fun createForInstruction(
        wallet: GreenWallet,
        session: GdkSession,
        isAutoSwap: Boolean,
        account: Account,
        input: String,
        amountSats: Long?,
        refundAddress: String,
        xPubHashId: String,
    ): SwapDetails {
        val dedupKey = instructionDedupKey(input, amountSats)

        val existing = database.getSwapFromUnpaidInvoice(invoice = dedupKey, xPubHashId = xPubHashId)
        val swap = existing?.takeIf { !it.is_magic }?.letTryCatch {
            session.lwk.restorePreparePay(it.data_)
        } ?: run {
            val newSwap = session.lwk.createNormalSubmarineSwap(
                input = input,
                refundAddress = refundAddress,
                amountSats = amountSats,
            )
            database.setSwap(
                id = newSwap.swapId(),
                walletId = wallet.id,
                xPubHashId = xPubHashId,
                invoice = dedupKey,
                swapType = SwapType.NormalSubmarine,
                isAutoSwap = isAutoSwap,
                isMagic = false,
                data = newSwap.serialize(),
            )
            newSwap
        }

        return SwapDetails(
            swapId = swap.swapId(),
            address = swap.uriAddress().toString(),
            fromAmount = swap.uriAmount().toLong(),
            fromAssetId = account.network.policyAsset,
            submarineInvoiceTo = input,
            toAmount = amountSats ?: 0,
            toAssetId = session.lightning.policyAsset,
            providerFee = swap.boltzFee()?.toLong() ?: 0,
            claimNetworkFee = (swap.fee()?.toLong() ?: 0) - (swap.boltzFee()?.toLong() ?: 0),
        )
    }

    private fun instructionDedupKey(input: String, amountSats: Long?): String =
        "$input|amountSats=$amountSats"
}

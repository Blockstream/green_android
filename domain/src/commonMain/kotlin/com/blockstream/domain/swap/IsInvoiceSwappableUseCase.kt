package com.blockstream.domain.swap

import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.lwk.PaymentInstruction

/**
 * Result of evaluating whether a Lightning payment instruction can be used for a submarine swap.
 *
 * Distinguishing failure modes lets callers surface a precise error string instead of a
 * generic "invalid address" when the input is recognised but unsupported (e.g. amountless BOLT11).
 */
sealed interface InvoiceSwappability {
    /** Recognised and ready to swap (BOLT11 with amount, LNURL, or any BOLT12 offer). */
    data object Swappable : InvoiceSwappability

    /** Recognised as BOLT11 but without an embedded amount. Submarine swaps need a fixed amount. */
    data object AmountlessBolt11 : InvoiceSwappability

    /** Unparseable, unrecognised, or not a Lightning instruction. */
    data object Unknown : InvoiceSwappability
}

/**
 * Use case that classifies a Lightning payment instruction for swap eligibility.
 *
 * Accepts:
 *  - BOLT11 invoices with an explicit amount.
 *  - LNURL / Lightning Address (amount supplied later).
 *  - BOLT12 offers, amountless or per-item (amount/count supplied later).
 *
 * Rejects amountless BOLT11 explicitly so callers can show a meaningful error.
 */
class IsInvoiceSwappableUseCase {

    suspend operator fun invoke(
        address: String,
        session: GdkSession
    ): InvoiceSwappability {
        val normalized = address.replace("lightning:", "")
        val instruction = tryCatch { session.lwkOrNull?.inspectPaymentInstruction(normalized) }
        return when (instruction) {
            is PaymentInstruction.LnUrl, is PaymentInstruction.Bolt12 -> InvoiceSwappability.Swappable
            is PaymentInstruction.Bolt11 ->
                if (instruction.amountSats != null) InvoiceSwappability.Swappable
                else InvoiceSwappability.AmountlessBolt11
            null -> InvoiceSwappability.Unknown
        }
    }
}

package com.blockstream.domain.send

import breez_sdk.InputType
import com.blockstream.data.data.Denomination
import com.blockstream.data.extensions.getSafeQueryParameter
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.lightning.amountSatoshi
import com.blockstream.data.utils.UserInput
import com.eygraber.uri.toKmpUriOrNull

/**
 * Extracts a send amount (in satoshis) from a raw user [input].
 *
 * Supported inputs: on‑chain Bitcoin, Liquid URIs, and Lightning BOLT11 invoices. Rules:
 *
 * - Lightning (BOLT11): if the invoice encodes an amount, return it in satoshis; otherwise `null`.
 * - Bitcoin on‑chain: read the `amount` query parameter (e.g., `bitcoin:addr?amount=0.001`),
 *   parse and convert it to satoshis when present; otherwise `null`.
 * - Liquid: if `assetid` is missing, return `null` (upstream should prompt for asset selection).
 *   If `assetid` is present, parse the `amount` query parameter and return its satoshis when
 *   available; otherwise `null`.
 *
 * Parsing details:
 * - Uses the active [GdkSession] for network detection (`parseInput`).
 * - Query parameters are read with helpers that support opaque URIs (e.g.,
 *   `liquidnetwork:lq1...?...`) via `toKmpUriOrNull()` and `getSafeQueryParameter(...)`.
 * - Amount parsing is performed through `UserInput.parseUserInputSafe(..., denomination = BTC)` to
 *   normalize to satoshis.
 *
 * Error handling:
 * - Wrapped in `tryCatch { ... }`: returns `null` on invalid or unsupported inputs rather than
 *   throwing.
 *
 * Thread-safety: read-only; safe to call from coroutines.
 */
class GetSendAmountUseCase() {

    /**
     * Extracts an amount in satoshis from the provided send [input].
     *
     * Parsing is delegated to the active [GdkSession] to detect the network, while URI
     * query parameters (e.g., `amount`, `assetid`) are read using safe helpers that work
     * for opaque URIs.
     *
     * Behavior summary:
     * - Lightning BOLT11 with amount → returns that amount in satoshis.
     * - Lightning BOLT11 without amount → returns `null`.
     * - Bitcoin on‑chain with `amount` → parses to satoshis and returns it; otherwise `null`.
     * - Liquid without `assetid` → returns `null` to force asset selection upstream.
     * - Liquid with `assetid` and `amount` → parses to satoshis and returns it; otherwise `null`.
     *
     * @param session the current wallet/session context used for parsing
     * @param input a raw address/payment request/URI string
     * @return the parsed amount in satoshis, or `null` if the input has no amount or is invalid
     */
    suspend operator fun invoke(session: GdkSession, input: String): Long? {

        return tryCatch {
            val parsed = session.parseInput(input)

            val network = parsed?.first
            val inputType = parsed?.second

            if (network != null) {

                if (network.isLightning) {
                    if (inputType is InputType.Bolt11) {
                        inputType.invoice.amountSatoshi()
                    } else null
                } else {
                    val assetId = tryCatch { input.toKmpUriOrNull()?.getSafeQueryParameter("assetid") }
                    val amount = tryCatch { input.toKmpUriOrNull()?.getSafeQueryParameter("amount") }

                    if (network.isLiquid && assetId == null) return@tryCatch null

                    UserInput.parseUserInputSafe(
                        session = session,
                        input = amount,
                        assetId = assetId,
                        denomination = Denomination.BTC
                    ).getBalance()?.satoshi
                }
            } else null
        }
    }
}
